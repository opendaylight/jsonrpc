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
import static org.junit.Assert.fail;
import static org.opendaylight.jsonrpc.impl.Util.store2int;
import static org.opendaylight.jsonrpc.impl.Util.store2str;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.messagelib.DefaultTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.ResponderSession;
import org.opendaylight.jsonrpc.bus.messagelib.SubscriberSession;
import org.opendaylight.jsonrpc.bus.messagelib.TestHelper;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.model.DataOperationArgument;
import org.opendaylight.jsonrpc.model.DeleteListenerArgument;
import org.opendaylight.jsonrpc.model.ListenerKey;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.jsonrpc.model.StoreOperationArgument;
import org.opendaylight.jsonrpc.model.TxArgument;
import org.opendaylight.jsonrpc.model.TxOperationArgument;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteControlTest extends AbstractJsonRpcTest {
    private static final String TOPO_TP_DATA = "{\"network-topology:network-topology\":"
            + "{\"topology\":[{\"topology-id\":\"topology1\",\"node\":[{\"node-id\":\"node1\","
            + "\"termination-point\":[{\"tp-id\": \"eth0\"}]}]}]}}";
    private static final String TEST_MODEL_PATH = "{\"test-model:top-element\":{}}";
    private static final String MLX_JSON_PATH =
            "{\"jsonrpc:config\":{ \"configured-endpoints\" : [ { \"name\" : \"lab-mlx\"} ]}}";
    private static final String MLX_CONFIG_DATA = "{\"name\":\"lab-mlx\", \"modules\": [\"ietf-inet-types\", "
            + "\"brocade-mlx-interfaces\", \"brocade-mlx-router\", \"brocade-mlx-security\", \"brocade-mlx-types\"]}";
    private static final String ENTITY = "test-model";
    private static final Logger LOG = LoggerFactory.getLogger(RemoteControlTest.class);
    private RemoteControl ctrl;
    private JsonParser parser;
    private JsonConverter conv;
    private TransportFactory transportFactory;
    private JsonRpcPathCodec codec;

    @Before
    public void setUp() throws Exception {
        transportFactory = new DefaultTransportFactory();
        ctrl = new RemoteControl(getDomBroker(), schemaContext, transportFactory, getDOMNotificationRouter(),
                getDOMRpcRouter().getRpcService());

        parser = new JsonParser();
        conv = new JsonConverter(schemaContext);
        codec = JsonRpcPathCodec.create(schemaContext);
        logTestName("START");
    }

    @After
    public void tearDown() throws Exception {
        logTestName("END");
        ctrl.close();
    }

    @Test
    public void testCRUD() throws Exception {
        JsonElement path = parser.parse(TEST_MODEL_PATH);
        String txId = ctrl.txid();
        ctrl.put(new DataOperationArgument(txId, "1", ENTITY, path,
                parser.parse("{ \"test-model:top-element\" : { \"level2a\" : {}}}")));
        assertTrue(ctrl.commit(new TxArgument(txId)));
        assertTrue(ctrl.exists(new StoreOperationArgument("1", ENTITY, path)));
        txId = ctrl.txid();
        ctrl.delete(new TxOperationArgument(txId, "1", ENTITY, path));
        assertTrue(ctrl.commit(new TxArgument(txId)));
        assertFalse(ctrl.exists(new StoreOperationArgument("1", ENTITY, path)));
    }

    @Test
    public void testCRUD2() throws Exception {
        JsonElement path = parser.parse(TEST_MODEL_PATH);
        String txId = ctrl.txid();
        ctrl.put(new DataOperationArgument(txId, "1", ENTITY, path,
                parser.parse("{ \"test-model:top-element\" : { \"level2a\" : { \"abc\" : \"123\"}}}")));
        assertFalse(ctrl.exists(new StoreOperationArgument("1", ENTITY, path)));
        assertTrue(ctrl.commit(new TxArgument(txId)));
        assertTrue(ctrl.exists(new StoreOperationArgument("1", ENTITY, path)));
    }

    @Test
    public void testCommitNonExistentTX() throws Exception {
        assertFalse(ctrl.commit(new TxArgument(UUID.randomUUID().toString())));
    }

    @Test
    public void testCancelNonExistentTX() throws Exception {
        assertFalse(ctrl.cancel(new TxArgument(UUID.randomUUID().toString())));
    }

    /**
     * This is quite complex test scenario. First, lets create
     * {@link NetworkTopology} with some node in it. Then, write it to
     * datastore. Lastly, use {@link RemoteControl} to read this data using
     * JSON-RPC request.
     */
    @Test
    public void testReadTopologyData() throws Exception {
        final Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> e = TestUtils.getMockTopologyAsDom(getCodec());
        final DOMDataTreeWriteTransaction wtx = getDomBroker().newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.OPERATIONAL, e.getKey(), e.getValue());
        wtx.commit().get();

        InstanceIdentifier<NetworkTopology> nii = InstanceIdentifier.create(NetworkTopology.class);

        YangInstanceIdentifier yii = getCodec().toYangInstanceIdentifier(nii);
        dumpYii(yii);

        final JsonElement path = conv.toBus(yii, null).getPath();
        final YangInstanceIdentifier parsedYii = codec.deserialize(path.getAsJsonObject());
        assertEquals(yii, parsedYii);

        LOG.info("JSON path : {}", path);
        LOG.info("path from codec : {}", yii);

        assertNotNull(ctrl.read(new StoreOperationArgument(store2str(store2int(LogicalDatastoreType.OPERATIONAL)),
                "test-model", path)));
    }

    /**
     * Test path to leaf in container.
     */
    @Test
    public void testPathToLeaf() {
        final YangInstanceIdentifier yii = YangInstanceIdentifier.builder()
                .node(NetworkTopology.QNAME)
                .node(Topology.QNAME)
                .nodeWithKey(Topology.QNAME, QName.create(Topology.QNAME, "topology-id"), "topology1")
                .node(QName.create(Topology.QNAME, "server-provided"))
                .build();
        LOG.info("YII : {}", yii);
        final YangInstanceIdentifier ii = codec.deserialize(parser
                .parse("{\"network-topology:network-topology\": "
                        + "{\"topology\": [{\"topology-id\": \"topology1\",\"server-provided\": {}}]}}")
                .getAsJsonObject());
        assertEquals(yii, ii);
    }

    /**
     * Test path to item in list.
     */
    @Test
    public void testPathToListItem() {
        final YangInstanceIdentifier yii = YangInstanceIdentifier.builder().node(NetworkTopology.QNAME)
                .node(Topology.QNAME)
                .nodeWithKey(Topology.QNAME, QName.create(Topology.QNAME, "topology-id"), "topology1").node(Node.QNAME)
                .nodeWithKey(Node.QNAME, QName.create(Node.QNAME, "node-id"), "node1").node(TerminationPoint.QNAME)
                .nodeWithKey(TerminationPoint.QNAME, QName.create(TerminationPoint.QNAME, "tp-id"), "eth0").build();
        LOG.info("YII : {}", yii);
        final YangInstanceIdentifier ii = codec.deserialize(parser.parse(TOPO_TP_DATA).getAsJsonObject());
        assertEquals(yii, ii);
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testInvalidPaths() {
        //@formatter:off
        final String[] paths = new String[] {
            "{\"network-topology:network-topology\":{\"topology\":[1]}}",
            "{\"network-topology:network-topology\":{\"topology\":[[]]}}",
            "{\"network-topology:network-topology\":null}",
            "{\"non-existent-module:data1\":{}}"
        };
        //@formatter:off
        for (final String p : paths) {
            try {
                codec.deserialize(parser.parse(p).getAsJsonObject());
                fail("This path should not be parseable !  : " + p);
            } catch (RuntimeException e) {
                LOG.info("This was expected : " + e.getMessage());
            }
        }
    }

    @Test
    public void testTxCancel() {
        UUID uuid = UUID.fromString(ctrl.txid());
        assertTrue(ctrl.cancel(new TxArgument(uuid.toString())));
        assertFalse(ctrl.cancel(new TxArgument(uuid.toString())));
    }

    /**
     * This test doesn't test anything, it is used as reference for other tests (merging related).
     */
    @Test
    public void testMerge() throws OperationFailedException, InterruptedException, ExecutionException {
        DOMDataTreeWriteTransaction wtx = getDomBroker().newWriteOnlyTransaction();
        Config c1 = new ConfigBuilder().setWhoAmI(new Uri("urn:bla"))
                .setConfiguredEndpoints(Lists.<ConfiguredEndpoints>newArrayList()).build();

        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> e1 = getCodec()
                .toNormalizedNode(InstanceIdentifier.create(Config.class), c1);

        wtx.put(LogicalDatastoreType.CONFIGURATION, e1.getKey(), e1.getValue());
        wtx.commit().get();
        ConfiguredEndpoints c2 = new ConfiguredEndpointsBuilder().setName("name-1")
                .setModules(Lists.newArrayList(new YangIdentifier("ietf-inet-types"))).build();

        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> e2 = getCodec().toNormalizedNode(InstanceIdentifier
                .builder(Config.class).child(ConfiguredEndpoints.class, new ConfiguredEndpointsKey("name-1")).build(),
                c2);

        wtx = getDomBroker().newWriteOnlyTransaction();
        LOG.info("Merging data {} at path {}", e2.getValue(), e2.getKey());
        wtx.merge(LogicalDatastoreType.CONFIGURATION, e2.getKey(), e2.getValue());
        wtx.commit().get();
    }

    @Test
    public void testReducedData() {
        YangInstanceIdentifier yii = codec.deserialize(parser.parse(MLX_JSON_PATH).getAsJsonObject());
        assertNotNull(conv.jsonElementToNormalizedNode(parser.parse(MLX_CONFIG_DATA), yii, true));
    }

    /**
     * Here list item data are merged into non-existent list.
     */
    @Test
    public void testMergeListItemNonExistentList() throws InterruptedException {
        String txId = ctrl.txid();
        ctrl.merge(new DataOperationArgument(txId, store2str(store2int(LogicalDatastoreType.CONFIGURATION)), "",
                parser.parse(MLX_JSON_PATH),
                parser.parse(MLX_CONFIG_DATA)));
        assertTrue(ctrl.commit(new TxArgument(txId)));
    }

    @Test
    public void testTxPutMergeDelete() throws Exception {
        //@formatter:off
        UUID uuid = UUID.fromString(ctrl.txid());

        ctrl.put(new DataOperationArgument(uuid.toString(),
                store2str(store2int(LogicalDatastoreType.OPERATIONAL)),
                "test-model", // entity
                parser.parse(TEST_MODEL_PATH), // path
                parser.parse("{\"test-model:top-element\":{\"level2a\":{}}}"))); // data

        ctrl.commit(new TxArgument(uuid.toString()));

        uuid = UUID.randomUUID();
        ctrl.put(new DataOperationArgument(uuid.toString(),
                store2str(store2int(LogicalDatastoreType.CONFIGURATION)),
                "something",
                parser.parse("{\"jsonrpc:config\":{}}"),
                parser.parse("{\"jsonrpc:config\":{\"configured-endpoints\":[]}}")));
        ctrl.commit(new TxArgument(uuid.toString()));

        uuid = UUID.randomUUID();
        ctrl.merge(new DataOperationArgument(uuid.toString(),
                store2str(store2int(LogicalDatastoreType.CONFIGURATION)),
                "something",
                parser.parse(MLX_JSON_PATH),
                parser.parse(MLX_CONFIG_DATA)));
        ctrl.commit(new TxArgument(uuid.toString()));

        assertTrue(ctrl.exists(new StoreOperationArgument(store2str(store2int(LogicalDatastoreType.OPERATIONAL)),
                "test-model", // entity
                parser.parse(TEST_MODEL_PATH)))); // path

        uuid = UUID.randomUUID();
        ctrl.delete(new TxOperationArgument(uuid.toString(),
                store2str(store2int(LogicalDatastoreType.OPERATIONAL)),
                "test-model", // entity
                parser.parse(TEST_MODEL_PATH))); // path

        ctrl.commit(new TxArgument(uuid.toString()));

        assertTrue(ctrl.error(new TxArgument(uuid.toString())).isEmpty());

        assertFalse(ctrl.exists(new StoreOperationArgument(store2str(store2int(LogicalDatastoreType.OPERATIONAL)),
                "test-model", // entity
                parser.parse(TEST_MODEL_PATH) // path
                )));
        //@formatter:on
    }

    @Test
    public void testPutWithoutTx() {
        ctrl.put(new DataOperationArgument(UUID.randomUUID().toString(),
                store2str(store2int(LogicalDatastoreType.OPERATIONAL)), "test-model", parser.parse(TEST_MODEL_PATH),
                parser.parse("{ \"test-model:top-element\" : { \"level2a\" : {}}}")));
    }

    @Test
    public void testPutReducedDataForm() {
        final String uuid = UUID.randomUUID().toString();
        ctrl.put(new DataOperationArgument(uuid, store2str(store2int(LogicalDatastoreType.CONFIGURATION)), "test-model",
                parser.parse("{\"test-model:grillconf\":{}}"), parser.parse("{\"gasKnob\":10}")));
        assertTrue(ctrl.commit(new TxArgument(uuid)));
    }

    @Test(timeout = 30_000)
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void testDcn() throws URISyntaxException, InterruptedException, IOException {
        final CountDownLatch latch = new CountDownLatch(1);
        final JsonElement path = parser.parse(TEST_MODEL_PATH);
        final int port = TestHelper.getFreeTcpPort();
        // expose RemoteControl
        final ResponderSession resp = transportFactory.createResponder(TestHelper.getBindUri("zmq", port), ctrl, true);
        // create requester proxy
        final RemoteOmShard req = transportFactory.createRequesterProxy(RemoteOmShard.class,
                TestHelper.getConnectUri("zmq", port), true);
        final ListenerKey listener = req.addListener("config", ENTITY, path);
        LOG.info("Publisher at {}", listener);

        final SubscriberSession toClose = transportFactory.createSubscriber(listener.getUri(),
                new DcnPublisherImpl(latch), true);
        String txId = ctrl.txid();
        ctrl.put(new DataOperationArgument(txId, "0", ENTITY, path,
                parser.parse("{ \"test-model:top-element\" : { \"level2a\" : { \"abc\" : \"123\"}}}")));
        ctrl.commit(new TxArgument(txId));
        txId = ctrl.txid();
        ctrl.delete(new TxOperationArgument(txId, "0", ENTITY, path));
        ctrl.commit(new TxArgument(txId));

        latch.await(15, TimeUnit.SECONDS);
        assertTrue(req.deleteListener(new DeleteListenerArgument(listener.getUri(), listener.getName())));
        req.close();
        toClose.close();
        resp.close();
    }

    @Test
    public void testRemoveNonExistentDcn() {
        assertFalse(ctrl.deleteListener(new DeleteListenerArgument("","")));
    }

    /*
     * Helpers and utilities
     */

    private static void dumpYii(YangInstanceIdentifier yii) {
        final List<PathArgument> path = yii.getPathArguments();
        int index = 0;
        LOG.info("Path len : {}", path.size());
        for (final PathArgument p : path) {
            LOG.info("{}{} : {}", Strings.repeat("-", index++), p.getNodeType(), p);
        }
    }
}
