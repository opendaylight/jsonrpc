/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.gson.JsonElement;

import java.net.URISyntaxException;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumHashMap;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.hmap.JsonPathCodec;
import org.opendaylight.jsonrpc.model.MutablePeer;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.mdsal.common.api.AsyncTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionChain;
import org.opendaylight.mdsal.common.api.TransactionChainListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * End-to-end test connecting {@link JsonRPCDataBroker}, {@link JsonRPCTx} and
 * {@link RemoteOmShard}.
 *
 * <p>
 * Goal is to make sure that behavior of {@link JsonRPCDataBroker} is consistent
 * with {@link DOMDataBroker}
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public class JsonRPCTE2ETest extends AbstractJsonRpcTest {
    private JsonRPCDataBroker jrbroker;
    private MutablePeer peer;
    private RemoteGovernance governance;
    private RemoteOmShard shard;
    private TransportFactory transportFactory;
    private ScheduledExecutorService exec;
    private final HierarchicalEnumMap<JsonElement, DataType, String> pathMap = HierarchicalEnumHashMap
            .create(DataType.class, JsonPathCodec.create());

    @Before
    public void setUp() throws URISyntaxException {
        exec = Executors.newScheduledThreadPool(1);
        NormalizedNodesHelper.init(schemaContext);
        transportFactory = mock(TransportFactory.class);
        shard = new RemoteControl(getDomBroker(), schemaContext,
                exec, transportFactory);
        peer = new MutablePeer();
        peer.name("test");
        governance = mock(RemoteGovernance.class);
        doReturn(shard).when(transportFactory).createRequesterProxy(any(), anyString(), anyLong());
        pathMap.put(jsonParser.parse("{\"network-topology:network-topology\":{}}"),
                DataType.OPERATIONAL_DATA, "zmq://localhost");

        jrbroker = new JsonRPCDataBroker(peer, schemaContext, pathMap, transportFactory, governance,
                new JsonConverter(schemaContext));
        logTestName("START");
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException {
        logTestName("END");
        final DOMDataTreeWriteTransaction wtx = getDomBroker().newWriteOnlyTransaction();
        wtx.delete(LogicalDatastoreType.OPERATIONAL, yiiFromJson("{ \"network-topology:network-topology\": {}}"));
        wtx.commit().get();
        exec.shutdown();
    }

    @Test
    public void testRead() throws Exception {
        // Write data to DOM DS
        final Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> e = TestUtils.getMockTopologyAsDom(schemaContext);
        final DOMDataTreeWriteTransaction wtx = getDomBroker().newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.OPERATIONAL, e.getKey(), e.getValue());
        wtx.commit().get();

        // Read using JSON-RPC databroker
        final DOMDataTreeReadTransaction rtx = jrbroker.newReadOnlyTransaction();
        final Optional<NormalizedNode<?, ?>> result = rtx.read(LogicalDatastoreType.OPERATIONAL, e.getKey()).get();
        assertNotNull(result.get());
    }

    @Test
    public void testWrite() throws Exception {
        // Write using JSON-RPC databroker
        final Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> e = TestUtils.getMockTopologyAsDom(schemaContext);
        final DOMDataTreeWriteTransaction wtx = jrbroker.newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.OPERATIONAL, e.getKey(), e.getValue());
        wtx.commit().get();

        // Read using DOM databroker
        final DOMDataTreeReadTransaction rtx = getDomBroker().newReadOnlyTransaction();
        final Optional<NormalizedNode<?, ?>> result = rtx.read(LogicalDatastoreType.OPERATIONAL, e.getKey()).get();
        assertNotNull(result.get());
    }

    @Test
    public void testReadWriteTransaction() throws Exception {
        final Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> e = TestUtils.getMockTopologyAsDom(schemaContext);
        final DOMDataTreeReadWriteTransaction rwtx = jrbroker.newReadWriteTransaction();
        rwtx.put(LogicalDatastoreType.OPERATIONAL, e.getKey(), e.getValue());
        Optional<NormalizedNode<?,?>> result = rwtx.read(LogicalDatastoreType.OPERATIONAL, e.getKey()).get();
        rwtx.commit().get();
        assertNotNull(result.get());
    }

    /*
     * Expected error due to first TX is not yet finished
     */
    @Test(expected = IllegalStateException.class)
    public void testUnfinishedTxChain() throws InterruptedException {
        final DOMTransactionChain chain = jrbroker.createTransactionChain(new TransactionChainListener() {
            @Override
            public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
                // NOOP
            }

            @Override
            public void onTransactionChainFailed(TransactionChain<?, ?> chain, AsyncTransaction<?, ?> transaction,
                    Throwable cause) {
                // NOOP
            }
        });
        chain.newWriteOnlyTransaction();
        chain.newWriteOnlyTransaction();
    }

    @Test
    public void testTxChain() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> e = TestUtils.getMockTopologyAsDom(schemaContext);
        final DOMTransactionChain chain = jrbroker.createTransactionChain(new TransactionChainListener() {
            @Override
            public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
                latch.countDown();
            }

            @Override
            public void onTransactionChainFailed(TransactionChain<?, ?> chain, AsyncTransaction<?, ?> transaction,
                    Throwable cause) {
            }
        });
        final DOMDataTreeWriteTransaction tx1 = chain.newWriteOnlyTransaction();
        tx1.put(LogicalDatastoreType.OPERATIONAL, e.getKey(), e.getValue());
        tx1.commit();
        final DOMDataTreeWriteTransaction tx2 = chain.newWriteOnlyTransaction();
        tx2.put(LogicalDatastoreType.OPERATIONAL, e.getKey(), e.getValue());
        tx2.commit();
        chain.close();
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    private YangInstanceIdentifier yiiFromJson(String json) {
        return YangInstanceIdentifierDeserializer.toYangInstanceIdentifier(jsonParser.parse(json), schemaContext);
    }
}
