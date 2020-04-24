/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.messagelib.DefaultTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.MessageLibrary;
import org.opendaylight.jsonrpc.bus.messagelib.ResponderSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumHashMap;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.hmap.JsonPathCodec;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.FactorialInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.FactorialOutput;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link JsonRPCtoRPCBridge}.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public class JsonRPCtoRPCBridgeAsyncTest extends AbstractJsonRpcTest {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCtoRPCBridgeAsyncTest.class);
    private JsonRPCtoRPCBridge bridge;
    private MessageLibrary messaging;
    private BindingToNormalizedNodeCodec bi2baCodec;
    private Module mod;
    private int rpcResponderPort = -1;
    private ResponderSession rpcResponder;
    private TransportFactory transportFactory;
    private HierarchicalEnumMap<JsonElement, DataType, String> pathMap;
    private static final String TRANSPORT = "zmq";

    @Before
    public void setUp() throws Exception {
        pathMap = HierarchicalEnumHashMap.create(DataType.class, JsonPathCodec.create());
        rpcResponderPort = getFreeTcpPort();
        startTransport();
        NormalizedNodesHelper.init(schemaContext);
        bi2baCodec = NormalizedNodesHelper.getBindingToNormalizedNodeCodec();
        transportFactory = new DefaultTransportFactory();
        bridge = new JsonRPCtoRPCBridge(getPeer(), schemaContext, pathMap, mock(RemoteGovernance.class),
                transportFactory, new JsonConverter(schemaContext));
        mod = schemaContext.findModule("test-model", Revision.of("2016-11-17")).get();
    }

    @After
    public void tearDown() throws Exception {
        bridge.close();
        stopTransport();
        transportFactory.close();
    }

    @Test(timeout = 15_000)
    public void test() throws Exception {
        final SchemaPath path = rpcPath(mod, "factorial");

        // BA => BI
        final ContainerNode rpcDef = prepareRpcInput(new FactorialInputBuilder().setInNumber(8).build());

        final DOMRpcResult result = bridge.invokeRpc(path, rpcDef).get();
        logResult(result);

        // BI => BA
        final FactorialOutput out = extractRpcOutput(result, FactorialOutput.class, "factorial", mod);
        LOG.info("DataObject : {}", out);
        assertEquals(40320L, (long) out.getOutNumber());
    }


    ///////////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////////
    private void logResult(DOMRpcResult result) {
        LOG.info("Result : {}", result.getResult());
        LOG.info("Errors : {}", result.getErrors());
    }

    private <T extends DataContainer> ContainerNode prepareRpcInput(T dataObject) {
        return bi2baCodec.toNormalizedNodeRpcData(dataObject);
    }

    @SuppressWarnings("unchecked")
    private <T> T extractRpcOutput(DOMRpcResult result, Class<T> outputType, String rpcName, Module module) {
        SchemaPath path2 = SchemaPath.create(false, constructRpcQname(module, rpcName),
                constructRpcQname(module, "output"));
        return (T) bi2baCodec.fromNormalizedNodeRpcData(path2, (ContainerNode) result.getResult());

    }

    private Peer getPeer() {
        RpcEndpointsBuilder rpcEndpointsBuilder = new RpcEndpointsBuilder();
        rpcEndpointsBuilder.setEndpointUri(new Uri(String.format(TRANSPORT + "://localhost:%d", rpcResponderPort)));
        rpcEndpointsBuilder.setPath("{}");
        List<RpcEndpoints> list = new ArrayList<>();
        list.add(rpcEndpointsBuilder.build());
        return new ConfiguredEndpointsBuilder().setName("BlahBlah").setRpcEndpoints(list).build();
    }

    private void startTransport() {
        messaging = new MessageLibrary(TRANSPORT);
        rpcResponder = messaging.responder(String.format(TRANSPORT + "://0.0.0.0:%d", rpcResponderPort),
                new AsyncMockRpcHandler(), true);
    }

    private void stopTransport() {
        rpcResponder.close();
        messaging.close();
    }

    private static QName constructRpcQname(Module mod, String methodName) {
        return QName.create(mod.getQNameModule().getNamespace(), mod.getQNameModule().getRevision(), methodName);
    }

    private static SchemaPath rpcPath(Module mod, String methodName) {
        return SchemaPath.create(true, constructRpcQname(mod, methodName));
    }
}
