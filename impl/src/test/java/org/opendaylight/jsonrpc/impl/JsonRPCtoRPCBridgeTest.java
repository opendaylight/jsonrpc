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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.jsonrpc.bus.messagelib.MessageLibrary;
import org.opendaylight.jsonrpc.bus.messagelib.Session;
import org.opendaylight.jsonrpc.bus.messagelib.ThreadedSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.Util;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumHashMap;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.hmap.JsonPathCodec;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.model.RpcExceptionImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.FactorialInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.FactorialOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.GetAllNumbersInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.GetAllNumbersOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.MultiplyListInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.MultiplyListOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.MultiplyLlInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.MultiplyLlOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.TestModelService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.numbers.list.NumbersBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.SimpleDateFormatUtil;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;

/**
 * Tests for {@link JsonRPCtoRPCBridge}.
 * 
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public class JsonRPCtoRPCBridgeTest extends AbstractJsonRpcTest {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCtoRPCBridgeTest.class);
    private JsonRPCtoRPCBridge bridge;
    private MessageLibrary messaging;
    private BindingToNormalizedNodeCodec bi2baCodec;
    private Module mod;
    private int rpcResponderPort = -1;
    private ThreadedSession rpcResponder;
    private TransportFactory transportFactory;
    private HierarchicalEnumMap<JsonElement, DataType, String> pathMap;

    @Before
    public void setUp() throws Exception {
        pathMap = HierarchicalEnumHashMap.create(DataType.class, JsonPathCodec.create());
        rpcResponderPort = getFreeTcpPort();
        startZeroMq();
        NormalizedNodesHelper.init(schemaContext);
        bi2baCodec = NormalizedNodesHelper.getBindingToNormalizedNodeCodec();
        transportFactory = mock(TransportFactory.class);
        when(transportFactory.createSession(anyString())).thenAnswer(new Answer<Session>() {
            @Override
            public Session answer(InvocationOnMock invocation) throws Throwable {
                return Util.openSession((String) invocation.getArguments()[0], "REQ");
            }
        });
        bridge = new JsonRPCtoRPCBridge(getPeer(), schemaContext, pathMap, mock(RemoteGovernance.class),
                transportFactory);
        mod = schemaContext.findModuleByName("test-model",
                SimpleDateFormatUtil.getRevisionFormat().parse("2016-11-17"));
    }

    @After
    public void tearDown() {
        bridge.close();
        stopZeroMq();
    }

    /**
     * Test case : Invoke non-existent RPC method. <br />
     * Expected result : exception from RPC bridge
     *
     * @throws Exception
     */
    @Test(expected = NullPointerException.class)
    public void testRpcUnknownMethod() throws Exception {
        NormalizedNode<?, ?> rpcDef = ImmutableNodes.containerNode(constructRpcQname(mod, "unknown-method"));
        SchemaPath path = rpcPath(mod, "unknown-method");
        bridge.invokeRpc(path, rpcDef).checkedGet();
    }

    /**
     * Test case : Invoke simple method, no input and no output. <br />
     * Expected result : {@link DOMRpcResult} with no errors and no inner result
     *
     * @throws Exception
     */
    @Test
    public void testRpcSimpleMethod() throws Exception {
        NormalizedNode<?, ?> rpcDef = ImmutableNodes.containerNode(constructRpcQname(mod, "simple-method"));
        SchemaPath path = rpcPath(mod, "simple-method");
        DOMRpcResult result = bridge.invokeRpc(path, rpcDef).checkedGet();
        LOG.info("Simple RPC result : {}", result);
        assertTrue(result.getErrors().isEmpty());
        assertNull(result.getResult());
    }

    /**
     * Test case : Invoke RPC method with input and output parameters. details :
     * {@link TestModelService#MultiplyLlInput)} <br />
     * Expected result : {@link DOMRpcResult} with payload filled from remote
     * RPC service. This tests de/serialization of leaf-lists
     *
     * @throws Exception
     */
    @Test
    public void testRpcMultiplyLeafList() throws Exception {
        final SchemaPath path = rpcPath(mod, "multiply-ll");

        // BA => BI
        final ContainerNode rpcDef = prepareRpcInput(
                new MultiplyLlInputBuilder().setMultiplier((short) 3).setNumbers(Lists.newArrayList(2, 5, 7)).build());

        final DOMRpcResult result = bridge.invokeRpc(path, rpcDef).checkedGet();
        LOG.info("DOM RPC result : {}", result);
        assertTrue(result.getErrors().isEmpty());

        // BI => BA
        final MultiplyLlOutput out = extractRpcOutput(result, MultiplyLlOutput.class, "multiply-ll", mod);
        LOG.info("DataObject : {}", out);
        assertTrue(out.getNumbers().contains(15));
        assertTrue(out.getNumbers().contains(6));
        assertTrue(out.getNumbers().contains(21));
    }

    @Test
    public void testRpcFactorial() throws Exception {
        final SchemaPath path = rpcPath(mod, "factorial");

        // BA => BI
        final ContainerNode rpcDef = prepareRpcInput(new FactorialInputBuilder().setInNumber(8).build());

        final DOMRpcResult result = bridge.invokeRpc(path, rpcDef).checkedGet();
        LOG.info("DOM RPC result : {}", result);
        assertTrue(result.getErrors().isEmpty());

        // BI => BA
        final FactorialOutput out = extractRpcOutput(result, FactorialOutput.class, "factorial", mod);
        LOG.info("DataObject : {}", out);
        assertEquals(40320L, (long) out.getOutNumber());
    }

    /**
     * Test case : Invoke RPC method with input and output parameters. <br />
     * details : {@link TestModelService#MultiplyList()} <br />
     * Expected result : {@link DOMRpcResult} with payload filled from remote
     * RPC service. This tests de/serialization of list
     *
     * @throws Exception
     */
    @Test
    public void testRpcMultiplyList() throws Exception {
        final SchemaPath path = rpcPath(mod, "multiply-list");

        // BA => BI
        final ContainerNode rpcDef = prepareRpcInput(
                new MultiplyListInputBuilder().setMultiplier((short) 3)
                        .setNumbers(Lists.newArrayList(new NumbersBuilder().setNum(10).build(),
                                new NumbersBuilder().setNum(15).build(), new NumbersBuilder().setNum(17).build()))
                        .build());

        LOG.info("Transformed RPC NormalizedNode : {}", rpcDef);

        final DOMRpcResult result = bridge.invokeRpc(path, rpcDef).checkedGet();
        LOG.info("DOM RPC result : {}", result);
        assertTrue(result.getErrors().isEmpty());

        // BI => BA
        final MultiplyListOutput out = extractRpcOutput(result, MultiplyListOutput.class, "multiply-list", mod);
        LOG.info("DataObject : {}", out);
        assertEquals(3, out.getNumbers().size());
    }

    @Ignore
    @Test(expected = IllegalArgumentException.class)
    public void testMethodWithAnyXml() throws IOException, DOMRpcException {
        final SchemaPath path = rpcPath(mod, "method-with-anyxml");
        final NormalizedNodeResult resultHolder = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(resultHolder);
        final RpcDefinition rpcDef = mod.getRpcs().stream()
                .filter(r -> "method-with-anyxml".equals(r.getQName().getLocalName())).findFirst().get();
        try (final JsonParserStream jsonParser = JsonParserStream.create(writer, schemaContext, rpcDef)) {
            JsonReader reader = new JsonReader(new StringReader("{\"input\" : {\"some-data\" : null  }}"));
            jsonParser.parse(reader).flush();
        }
        bridge.invokeRpc(path, resultHolder.getResult()).checkedGet();
    }

    @Test
    public void testMethodWithAnyXmlNoData() throws IOException, DOMRpcException {
        final SchemaPath path = rpcPath(mod, "method-with-anyxml");
        final NormalizedNodeResult resultHolder = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(resultHolder);
        final RpcDefinition rpcDef = mod.getRpcs().stream()
                .filter(r -> "method-with-anyxml".equals(r.getQName().getLocalName())).findFirst().get();
        try (final JsonParserStream jsonParser = JsonParserStream.create(writer, schemaContext, rpcDef)) {
            JsonReader reader = new JsonReader(new StringReader("{\"input\" : { }}"));
            jsonParser.parse(reader).flush();
        }
        DOMRpcResult result = bridge.invokeRpc(path, resultHolder.getResult()).checkedGet();
        LOG.info("DOM RPC result : {}", result);
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testRpcComplexResponse() throws Exception {
        final SchemaPath path = rpcPath(mod, "get-all-numbers");
        final DOMRpcResult result = bridge.invokeRpc(path, prepareRpcInput(new GetAllNumbersInputBuilder().build()))
                .checkedGet();
        GetAllNumbersOutput out = extractRpcOutput(result, GetAllNumbersOutput.class, "get-all-numbers", mod);
        LOG.info("Output : {}", out);
    }

    /**
     * Test case : return error from RPC implementation. <br />
     * Verify that DOMRpcException is propagated from RPC bridge.
     *
     * @throws Exception
     */
    @Test(expected = RuntimeException.class)
    public void testRpcError() throws Exception {
        NormalizedNode<?, ?> rpcDef = ImmutableNodes.containerNode(constructRpcQname(mod, "error-method"));
        bridge.invokeRpc(rpcPath(mod, "error-method"), rpcDef).checkedGet();
    }

    @Ignore
    @Test
    public void test_AnyXmlFailure() throws IOException, DOMRpcException {
        final SchemaPath path = rpcPath(mod, "get-any-xml");
        final NormalizedNodeResult resultHolder = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(resultHolder);
        final RpcDefinition rpcDef = mod.getRpcs().stream()
                .filter(r -> "get-any-xml".equals(r.getQName().getLocalName())).findFirst().get();
        try (final JsonParserStream jsonParser = JsonParserStream.create(writer, schemaContext, rpcDef)) {
            JsonReader reader = new JsonReader(new StringReader("{\"input\" : {\"indata\" : 10  }}"));
            jsonParser.parse(reader).flush();
        }
        DOMRpcResult result = bridge.invokeRpc(path, resultHolder.getResult()).checkedGet();
        assertFalse(result.getErrors().isEmpty());
        assertNull(result.getResult());
    }

    @Test
    public void test_2LeafNodesInRpc() throws IOException, DOMRpcException {
        final SchemaPath path = rpcPath(mod, "removeCoffeePot");
        final NormalizedNode<?, ?> rpcDef = ImmutableNodes.containerNode(constructRpcQname(mod, "removeCoffeePot"));
        DOMRpcResult result = bridge.invokeRpc(path, rpcDef).checkedGet();
        LOG.info("Result : {}", result.getResult());
        assertTrue(result.getErrors().isEmpty());
        assertNotNull(result.getResult());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////////
    private <T extends DataContainer> ContainerNode prepareRpcInput(T dataObject) {
        return bi2baCodec.toNormalizedNodeRpcData(dataObject);
    }

    @SuppressWarnings("unchecked")
    private <T> T extractRpcOutput(DOMRpcResult result, Class<T> outputType, String rpcName, Module mod) {
        SchemaPath path2 = SchemaPath.create(false, constructRpcQname(mod, rpcName), constructRpcQname(mod, "output"));
        return (T) bi2baCodec.fromNormalizedNodeRpcData(path2, (ContainerNode) result.getResult());

    }

    private Peer getPeer() {
        RpcEndpointsBuilder rpcEndpointsBuilder = new RpcEndpointsBuilder();
        rpcEndpointsBuilder.setEndpointUri(new Uri(String.format("zmq://localhost:%d", rpcResponderPort)));
        rpcEndpointsBuilder.setPath("{}");
        List<RpcEndpoints> list = new ArrayList<>();
        list.add(rpcEndpointsBuilder.build());
        return new ConfiguredEndpointsBuilder().setName("BlahBlah").setRpcEndpoints(list).build();
    }

    private void startZeroMq() {
        messaging = new MessageLibrary("zmq");
        rpcResponder = messaging.threadedResponder(String.format("tcp://*:%d", rpcResponderPort), new MockRpcHandler());
    }

    private void stopZeroMq() {
        rpcResponder.stop();
    }

    private static QName constructRpcQname(Module mod, String methodName) {
        return QName.create(mod.getQNameModule().getNamespace(), mod.getQNameModule().getRevision(), methodName);
    }

    private static SchemaPath rpcPath(Module mod, String methodName) {
        return SchemaPath.create(true, constructRpcQname(mod, methodName));
    }
}
