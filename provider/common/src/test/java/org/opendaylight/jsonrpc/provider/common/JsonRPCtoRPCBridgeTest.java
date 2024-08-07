/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.messagelib.AbstractTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.DefaultTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.MessageLibrary;
import org.opendaylight.jsonrpc.bus.messagelib.ResponderSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.dom.codec.JsonReaderAdapter;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumHashMap;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.hmap.JsonPathCodec;
import org.opendaylight.jsonrpc.impl.JsonRPCtoRPCBridge;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.base.rev201014.numbers.list.NumbersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.FactorialInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.FactorialOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.GetAllNumbersInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.GetAllNumbersOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.MultiplyListInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.MultiplyListOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.MultiplyLlInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.MultiplyLlOutput;
import org.opendaylight.yangtools.binding.DataContainer;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
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
        transportFactory = new DefaultTransportFactory();
        startTransport();
        final Peer peer = getPeer();
        Util.populateFromEndpointList(pathMap, peer.getRpcEndpoints().values(), DataType.RPC);
        bridge = new JsonRPCtoRPCBridge(peer, schemaContext, pathMap, mock(RemoteGovernance.class), transportFactory,
                new JsonRpcCodecFactory(schemaContext));
        mod = schemaContext.findModule("test-model-rpc", Revision.of("2020-10-14")).orElseThrow();
        logTestName("START");
        TimeUnit.MILLISECONDS.sleep(250);
    }

    @After
    public void tearDown() throws Exception {
        logTestName("END");
        bridge.close();
        stopTransport();
        transportFactory.close();
    }

    /**
     * Test case : Invoke non-existent RPC method. <br />
     * Expected result : exception from RPC bridge
     */
    @Test(timeout = 15_000, expected = ExecutionException.class)
    public void testRpcUnknownMethod() throws InterruptedException, ExecutionException {
        ContainerNode rpcDef = ImmutableNodes.containerNode(constructRpcQname(mod, "unknown-method"));
        QName path = rpcPath(mod, "unknown-method");
        bridge.invokeRpc(path, rpcDef).get();
    }

    /**
     * Test case : Invoke simple method, no input and no output. <br />
     * Expected result : {@link DOMRpcResult} with no errors and no inner result
     */
    @Test(timeout = 15_000)
    public void testRpcSimpleMethod() throws Exception {
        ContainerNode rpcDef = ImmutableNodes.containerNode(constructRpcQname(mod, "simple-method"));
        QName path = rpcPath(mod, "simple-method");
        DOMRpcResult result = bridge.invokeRpc(path, rpcDef).get();
        LOG.info("Simple RPC result : {}", result);
        assertTrue(result.errors().isEmpty());
        assertNotNull(result.value());
    }

    /**
     * Test case : Invoke RPC method with input and output parameters. details :
     * {@link TestModelService#MultiplyLlInput}. <br />
     * Expected result : {@link DOMRpcResult} with payload filled from remote RPC service. This tests de/serialization
     * of leaf-lists
     */
    @Test(timeout = 15_000)
    public void testRpcMultiplyLeafList() throws Exception {
        final QName path = rpcPath(mod, "multiply-ll");
        // BA => BI
        final ContainerNode rpcDef = prepareRpcInput(
                new MultiplyLlInputBuilder().setMultiplier((short) 3).setNumbers(ImmutableSet.of(2, 5, 7)).build());

        final DOMRpcResult result = bridge.invokeRpc(path, rpcDef).get();
        LOG.info("DOM RPC result : {}", result);
        assertTrue(result.errors().isEmpty());

        // BI => BA
        final MultiplyLlOutput out = extractRpcOutput(result, MultiplyLlOutput.class, "multiply-ll", mod);
        LOG.info("DataObject : {}", out);
        assertTrue(out.getNumbers().contains(15));
        assertTrue(out.getNumbers().contains(6));
        assertTrue(out.getNumbers().contains(21));
    }

    @Test(timeout = 15_000)
    public void testRpcFactorial() throws Exception {
        final QName path = rpcPath(mod, "factorial");

        // BA => BI
        final ContainerNode rpcDef = prepareRpcInput(
                new FactorialInputBuilder().setInNumber(Uint16.valueOf(8)).build());

        final DOMRpcResult result = bridge.invokeRpc(path, rpcDef).get();
        logResult(result);

        // BI => BA
        final FactorialOutput out = extractRpcOutput(result, FactorialOutput.class, "factorial", mod);
        LOG.info("DataObject : {}", out);
        assertEquals(40320L, out.getOutNumber().longValue());
    }

    /**
     * Test case : Invoke RPC method with input and output parameters. <br />
     * details : {@link TestModelService#MultiplyList()} <br />
     * Expected result : {@link DOMRpcResult} with payload filled from remote RPC service. This tests de/serialization
     * of list
     */
    @Test(timeout = 15_000)
    public void testRpcMultiplyList() throws Exception {
        final QName path = rpcPath(mod, "multiply-list");

        // BA => BI
        final ContainerNode rpcDef = prepareRpcInput(new MultiplyListInputBuilder().setMultiplier((short) 3)
                .setNumbers(BindingMap.ordered(
                    new NumbersBuilder().setNum(10).build(),
                    new NumbersBuilder().setNum(15).build(),
                    new NumbersBuilder().setNum(17).build()))
                .build());

        LOG.info("Transformed RPC NormalizedNode : {}", rpcDef);

        final DOMRpcResult result = bridge.invokeRpc(path, rpcDef).get();
        logResult(result);
        assertTrue(result.errors().isEmpty());

        // BI => BA
        final MultiplyListOutput out = extractRpcOutput(result, MultiplyListOutput.class, "multiply-list", mod);
        LOG.info("DataObject : {}", out);
        assertEquals(3, out.getNumbers().size());
    }

    @Test(timeout = 15_000)
    public void testMethodWithAnyXmlNoData() throws Exception {
        final QName path = rpcPath(mod, "method-with-anyxml");
        final NormalizationResultHolder resultHolder = new NormalizationResultHolder();
        final NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(resultHolder);
        final RpcDefinition rpcDef = mod.getRpcs()
                .stream()
                .filter(r -> "method-with-anyxml".equals(r.getQName().getLocalName()))
                .findFirst()
                .orElseThrow();
        try (JsonParserStream parser = JsonParserStream.create(writer,
                JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.getShared(schemaContext),
                SchemaInferenceStack.of(schemaContext, Absolute.of(rpcDef.getQName())).toInference())) {
            parser.parse(JsonReaderAdapter.from(
                JsonParser.parseString("{\"input\" : { \"some-number\":5, \"some-data\": { \"data\" : 123}}}")));
        }
        DOMRpcResult result = bridge.invokeRpc(path, (ContainerNode) resultHolder.getResult().data()).get();
        logResult(result);
        assertTrue(result.errors().isEmpty());
    }

    @Test(timeout = 15_000)
    public void testRpcComplexResponse() throws Exception {
        final QName path = rpcPath(mod, "get-all-numbers");
        final DOMRpcResult result = bridge.invokeRpc(path, prepareRpcInput(new GetAllNumbersInputBuilder().build()))
                .get();
        GetAllNumbersOutput out = extractRpcOutput(result, GetAllNumbersOutput.class, "get-all-numbers", mod);
        LOG.info("Output : {}", out);
    }

    /**
     * Test case : return error from RPC implementation. <br />
     * Verify that DOMRpcException is propagated from RPC bridge.
     */
    @Test(timeout = 15_000)
    public void testRpcError() throws Exception {
        ContainerNode rpcDef = ImmutableNodes.containerNode(constructRpcQname(mod, "error-method"));
        DOMRpcResult result = bridge.invokeRpc(rpcPath(mod, "error-method"), rpcDef).get();
        logResult(result);
        assertFalse(result.errors().isEmpty());
    }

    @Test // (timeout = 15_000)
    public void test_2LeafNodesInRpc() throws Exception {
        final QName path = rpcPath(mod, "removeCoffeePot");
        final ContainerNode rpcDef = ImmutableNodes.containerNode(constructRpcQname(mod, "removeCoffeePot"));
        DOMRpcResult result = bridge.invokeRpc(path, rpcDef).get();
        logResult(result);
        assertTrue(result.errors().isEmpty());
        assertNotNull(result.value());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////////
    private static void logResult(DOMRpcResult result) {
        LOG.info("Result : {}", result.value());
        LOG.info("Errors : {}", result.errors());
    }

    private <T extends DataContainer> ContainerNode prepareRpcInput(T dataObject) {
        return getCodec().toNormalizedNodeRpcData(dataObject);
    }

    @SuppressWarnings("unchecked")
    private <T> T extractRpcOutput(DOMRpcResult result, Class<T> outputType, String rpcName, Module module) {
        Absolute path2 = Absolute.of(constructRpcQname(module, rpcName), constructRpcQname(module, "output"));
        return (T) getCodec().fromNormalizedNodeRpcData(path2, (ContainerNode) result.value());

    }

    private Peer getPeer() {
        final RpcEndpointsBuilder builder = new RpcEndpointsBuilder();
        builder.setEndpointUri(new Uri(String.format(TRANSPORT + "://localhost:%d", rpcResponderPort)));
        builder.setPath("{}");
        return new ConfiguredEndpointsBuilder().setName("BlahBlah")
                .setRpcEndpoints(BindingMap.of(builder.build()))
                .build();
    }

    private void startTransport() {
        messaging = ((AbstractTransportFactory) transportFactory).getMessageLibraryForTransport(TRANSPORT);
        rpcResponder = messaging.responder(String.format(TRANSPORT + "://0.0.0.0:%d", rpcResponderPort),
                new MockRpcHandler(), true);
        LOG.info("Started responder on port {}", rpcResponderPort);
    }

    private void stopTransport() {
        LOG.info("Stopping responder on port {}", rpcResponderPort);
        rpcResponder.close();
        messaging.close();
    }

    private static QName constructRpcQname(Module mod, String methodName) {
        return QName.create(mod.getQNameModule(), methodName);
    }

    private static QName rpcPath(Module mod, String methodName) {
        return constructRpcQname(mod, methodName);
    }
}
