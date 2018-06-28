/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
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
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private ResponderSession rpcResponder;
    private TransportFactory transportFactory;
    private HierarchicalEnumMap<JsonElement, DataType, String> pathMap;
    private static final String TRANSPORT = "zmq";
    private ScheduledExecutorService exec;
    @Rule
    public TestName nameRule = new TestName();

    @Before
    public void setUp() throws Exception {
        exec = Executors.newScheduledThreadPool(1);
        pathMap = HierarchicalEnumHashMap.create(DataType.class, JsonPathCodec.create());
        rpcResponderPort = getFreeTcpPort();
        startTransport();
        NormalizedNodesHelper.init(schemaContext);
        bi2baCodec = NormalizedNodesHelper.getBindingToNormalizedNodeCodec();
        transportFactory = new DefaultTransportFactory();
        bridge = new JsonRPCtoRPCBridge(getPeer(), schemaContext, pathMap, mock(RemoteGovernance.class),
                transportFactory, exec, new JsonConverter(schemaContext));
        mod = schemaContext.findModule("test-model", Revision.of("2016-11-17")).get();
    }

    @After
    public void tearDown() throws Exception {
        bridge.close();
        stopTransport();
        transportFactory.close();
        exec.shutdownNow();
    }

    /**
     * Test case : Invoke non-existent RPC method. <br />
     * Expected result : exception from RPC bridge
     */
    @Test(expected = ExecutionException.class, timeout = 15_000)
    public void testRpcUnknownMethod() throws InterruptedException, ExecutionException {
        logTestName();
        NormalizedNode<?, ?> rpcDef = ImmutableNodes.containerNode(constructRpcQname(mod, "unknown-method"));
        SchemaPath path = rpcPath(mod, "unknown-method");
        bridge.invokeRpc(path, rpcDef).get();
    }

    /**
     * Test case : Invoke simple method, no input and no output. <br />
     * Expected result : {@link DOMRpcResult} with no errors and no inner result
     */
    @Test(timeout = 15_000)
    public void testRpcSimpleMethod() throws Exception {
        logTestName();
        NormalizedNode<?, ?> rpcDef = ImmutableNodes.containerNode(constructRpcQname(mod, "simple-method"));
        SchemaPath path = rpcPath(mod, "simple-method");
        DOMRpcResult result = bridge.invokeRpc(path, rpcDef).get();
        LOG.info("Simple RPC result : {}", result);
        assertTrue(result.getErrors().isEmpty());
        assertNull(result.getResult());
    }

    /**
     * Test case : Invoke RPC method with input and output parameters. details :
     * {@link TestModelService#MultiplyLlInput}. <br />
     * Expected result : {@link DOMRpcResult} with payload filled from remote
     * RPC service. This tests de/serialization of leaf-lists
     */
    @Test(timeout = 15_000)
    public void testRpcMultiplyLeafList() throws Exception {
        logTestName();
        final SchemaPath path = rpcPath(mod, "multiply-ll");

        // BA => BI
        final ContainerNode rpcDef = prepareRpcInput(
                new MultiplyLlInputBuilder().setMultiplier((short) 3).setNumbers(Lists.newArrayList(2, 5, 7)).build());

        final DOMRpcResult result = bridge.invokeRpc(path, rpcDef).get();
        LOG.info("DOM RPC result : {}", result);
        assertTrue(result.getErrors().isEmpty());

        // BI => BA
        final MultiplyLlOutput out = extractRpcOutput(result, MultiplyLlOutput.class, "multiply-ll", mod);
        LOG.info("DataObject : {}", out);
        assertTrue(out.getNumbers().contains(15));
        assertTrue(out.getNumbers().contains(6));
        assertTrue(out.getNumbers().contains(21));
    }

    @Test(timeout = 15_000)
    public void testRpcFactorial() throws Exception {
        logTestName();
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

    /**
     * Test case : Invoke RPC method with input and output parameters. <br />
     * details : {@link TestModelService#MultiplyList()} <br />
     * Expected result : {@link DOMRpcResult} with payload filled from remote
     * RPC service. This tests de/serialization of list
     */
    @Test(timeout = 15_000)
    public void testRpcMultiplyList() throws Exception {
        logTestName();
        final SchemaPath path = rpcPath(mod, "multiply-list");

        // BA => BI
        final ContainerNode rpcDef = prepareRpcInput(
                new MultiplyListInputBuilder().setMultiplier((short) 3)
                        .setNumbers(Lists.newArrayList(new NumbersBuilder().setNum(10).build(),
                                new NumbersBuilder().setNum(15).build(), new NumbersBuilder().setNum(17).build()))
                        .build());

        LOG.info("Transformed RPC NormalizedNode : {}", rpcDef);

        final DOMRpcResult result = bridge.invokeRpc(path, rpcDef).get();
        logResult(result);
        assertTrue(result.getErrors().isEmpty());

        // BI => BA
        final MultiplyListOutput out = extractRpcOutput(result, MultiplyListOutput.class, "multiply-list", mod);
        LOG.info("DataObject : {}", out);
        assertEquals(3, out.getNumbers().size());
    }

    @Test(timeout = 15_000)
    public void testMethodWithAnyXmlNoData() throws Exception {
        logTestName();
        final SchemaPath path = rpcPath(mod, "method-with-anyxml");
        final NormalizedNodeResult resultHolder = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(resultHolder);
        final RpcDefinition rpcDef = mod.getRpcs()
                .stream()
                .filter(r -> "method-with-anyxml".equals(r.getQName().getLocalName()))
                .findFirst()
                .get();
        try (JsonParserStream jsonParser = JsonParserStream.create(writer,
                JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.getShared(schemaContext), rpcDef)) {
            JsonReader reader = new JsonReader(new StringReader("{\"input\" : { }}"));
            jsonParser.parse(reader).flush();
        }
        DOMRpcResult result = bridge.invokeRpc(path, resultHolder.getResult()).get();
        logResult(result);
        assertTrue(result.getErrors().isEmpty());
    }

    @Test(timeout = 15_000)
    public void testRpcComplexResponse() throws Exception {
        logTestName();
        final SchemaPath path = rpcPath(mod, "get-all-numbers");
        final DOMRpcResult result = bridge.invokeRpc(path, prepareRpcInput(new GetAllNumbersInputBuilder().build()))
                .get();
        GetAllNumbersOutput out = extractRpcOutput(result, GetAllNumbersOutput.class, "get-all-numbers", mod);
        LOG.info("Output : {}", out);
    }

    /**
     * Test case : return error from RPC implementation. <br />
     * Verify that DOMRpcException is propagated from RPC bridge.
     */
    @Test(expected = ExecutionException.class, timeout = 15_000)
    public void testRpcError() throws Exception {
        logTestName();
        NormalizedNode<?, ?> rpcDef = ImmutableNodes.containerNode(constructRpcQname(mod, "error-method"));
        bridge.invokeRpc(rpcPath(mod, "error-method"), rpcDef).get();
    }

    @Test(timeout = 15_000)
    public void test_2LeafNodesInRpc() throws Exception {
        logTestName();
        final SchemaPath path = rpcPath(mod, "removeCoffeePot");
        final NormalizedNode<?, ?> rpcDef = ImmutableNodes.containerNode(constructRpcQname(mod, "removeCoffeePot"));
        DOMRpcResult result = bridge.invokeRpc(path, rpcDef).get();
        logResult(result);
        assertTrue(result.getErrors().isEmpty());
        assertNotNull(result.getResult());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////////
    private void logResult(DOMRpcResult result) {
        LOG.info("Result : {}", result.getResult());
        LOG.info("Errors : {}", result.getErrors());
    }

    private void logTestName() {
        LOG.info("{}", Strings.repeat("=", 140));
        LOG.info("{}", nameRule.getMethodName());
        LOG.info("{}", Strings.repeat("=", 140));
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
                new MockRpcHandler());
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
