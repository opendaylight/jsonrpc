/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URISyntaxException;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumHashMap;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.hmap.JsonPathCodec;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link JsonRPCTx}.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class JsonRPCTxTest extends AbstractJsonRpcTest {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCTxTest.class);
    private static final String ENDPOINT = "zmq://localhost:1234";
    private static final String DEVICE_NAME = "dev-1";
    private JsonRPCTx trx;
    private JsonConverter conv;
    private RemoteOmShard om;
    private TransportFactory transportFactory;
    private HierarchicalEnumMap<JsonElement, DataType, String> pathMap;

    @Before
    public void setUp() throws URISyntaxException {
        pathMap = HierarchicalEnumHashMap.create(DataType.class, JsonPathCodec.create());
        pathMap.put(new JsonObject(), DataType.CONFIGURATION_DATA, ENDPOINT);
        pathMap.put(new JsonObject(), DataType.OPERATIONAL_DATA, ENDPOINT);
        transportFactory = mock(TransportFactory.class);
        NormalizedNodesHelper.init(schemaContext);
        om = mock(RemoteOmShard.class);
        doReturn(om).when(transportFactory).createRequesterProxy(any(), anyString());
        conv = new JsonConverter(schemaContext);
        trx = new JsonRPCTx(transportFactory, DEVICE_NAME, pathMap, conv, schemaContext);
    }

    @After
    public void tearDown() {
        trx.close();
        reset(om);
    }

    @Test
    public void testRead() throws Exception {
        final JsonElement elem = new JsonObject();
        doReturn(elem).when(om).read(eq(Util.store2str(Util.store2int(LogicalDatastoreType.OPERATIONAL))),
                eq(DEVICE_NAME), any(JsonElement.class));
        final FluentFuture<Optional<NormalizedNode<?, ?>>> fopt = trx
                .read(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.of(NetworkTopology.QNAME));

        final NormalizedNode<?, ?> nn = fopt.get(5, TimeUnit.SECONDS).get();
        LOG.info("Read output : {}", nn);
        assertEquals(NetworkTopology.QNAME.getNamespace().toString(), nn.getNodeType().getNamespace().toString());
        assertNotNull(nn.getValue());
    }

    @Test
    public void testReadNull() throws Exception {
        /* Special case - null read (allowed by RPC spec) should result in an empty container
         * and no barfs on the ODL side */
        final JsonElement elem = null;
        doReturn(elem).when(om).read(eq(Util.store2str(Util.store2int(LogicalDatastoreType.OPERATIONAL))),
                eq(DEVICE_NAME), any(JsonElement.class));
        final ListenableFuture<Optional<NormalizedNode<?, ?>>> fopt = trx
                .read(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.of(NetworkTopology.QNAME));

        final NormalizedNode<?, ?> nn = fopt.get(5, TimeUnit.SECONDS).get();
        LOG.info("Read output : {}", nn);
    }

    @Test
    public void testReadEmpty() throws InterruptedException, ExecutionException {
        doReturn(null).when(om).read(eq(Util.store2str(Util.store2int(LogicalDatastoreType.OPERATIONAL))),
                eq(DEVICE_NAME), any(JsonElement.class));
        assertFalse(trx.read(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.EMPTY).get().isPresent());
    }

    @Test
    public void testExists() throws Exception {
        doReturn(true).when(om).exists(anyString(), anyString(), any(JsonElement.class));
        assertTrue(trx.exists(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.of(NetworkTopology.QNAME))
                .get());
        verify(om, times(1)).exists(eq("config"), anyString(),
                any(JsonElement.class));
    }

    @Test
    public void testPut() throws InterruptedException, ExecutionException, TimeoutException {
        final Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> data = JsonConverterTest.createContainerNodeData();
        trx.put(LogicalDatastoreType.CONFIGURATION, data.getKey(), data.getValue());
        doReturn(true).when(om).commit(anyString());
        trx.commit().get(5, TimeUnit.SECONDS);
        verify(om, times(1)).put(anyString(), eq("config"), anyString(),
                any(JsonElement.class), any(JsonElement.class));
    }

    @Test
    public void testDelete() throws InterruptedException, ExecutionException, TimeoutException {
        doReturn(true).when(om).commit(anyString());
        trx.delete(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.of(NetworkTopology.QNAME));
        trx.commit().get(5, TimeUnit.SECONDS);
        verify(om, times(1)).delete(anyString(), eq("config"), anyString(),
                any(JsonElement.class));
        assertNotNull(trx.getIdentifier());
    }

    @Test
    public void testCancel() {
        trx.delete(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.of(NetworkTopology.QNAME));
        assertTrue(trx.cancel());
        verify(om, times(1)).delete(anyString(), eq("config"), anyString(),
                any(JsonElement.class));
        assertNotNull(trx.getIdentifier());
    }

    @Test
    public void testMerge() throws InterruptedException, ExecutionException, TimeoutException {
        final Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> data = JsonConverterTest.createContainerNodeData();
        doReturn(true).when(om).commit(anyString());
        trx.merge(LogicalDatastoreType.CONFIGURATION, data.getKey(), data.getValue());
        trx.commit().get(5, TimeUnit.SECONDS);
        verify(om, times(1)).merge(anyString(), eq("config"), anyString(),
                any(JsonElement.class), any(JsonElement.class));
    }

    @SuppressWarnings("checkstyle:AvoidHidingCauseException")
    @Test(expected = TransactionCommitFailedException.class)
    public void testSubmitFailure()
            throws InterruptedException, TimeoutException, ExecutionException, TransactionCommitFailedException {
        doReturn(false).when(om).commit(anyString());
        trx.delete(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.of(NetworkTopology.QNAME));
        try {
            trx.commit().get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TransactionCommitFailedException) {
                throw (TransactionCommitFailedException) e.getCause();
            }
            throw e;
        }
    }
}
