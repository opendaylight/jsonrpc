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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
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

@SuppressWarnings("deprecation")
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
    private ScheduledExecutorService exec;

    @Before
    public void setUp() {
        NormalizedNodesHelper.init(schemaContext);
        exec = Executors.newScheduledThreadPool(1);
        ctrl = new RemoteControl(getDomBroker(), schemaContext, NormalizedNodesHelper.getBindingToNormalizedNodeCodec(),
                500, exec);
        parser = new JsonParser();
        conv = new JsonConverter(schemaContext);
    }

    @After
    public void tearDown() throws Exception {
        ctrl.close();
        exec.shutdown();
    }

    @Test
    public void testCRUD() throws Exception {
        JsonElement path = parser.parse(TEST_MODEL_PATH);
        String txId = ctrl.txid();
        ctrl.put(txId, 1, ENTITY, path, parser.parse("{ \"test-model:top-element\" : { \"level2a\" : {}}}"));
        assertTrue(ctrl.commit(txId));
        assertTrue(ctrl.exists(1, ENTITY, path));
        txId = ctrl.txid();
        ctrl.delete(txId, 1, ENTITY, path);
        assertTrue(ctrl.commit(txId));
        assertFalse(ctrl.exists(1, ENTITY, path));
    }

    @Test
    public void testCRUD2() throws Exception {
        JsonElement path = parser.parse(TEST_MODEL_PATH);
        String txId = ctrl.txid();
        ctrl.put(txId, 1, ENTITY, path,
                parser.parse("{ \"test-model:top-element\" : { \"level2a\" : { \"abc\" : \"123\"}}}"));
        assertFalse(ctrl.exists(1, ENTITY, path));
        assertTrue(ctrl.commit(txId));
        assertTrue(ctrl.exists(1, ENTITY, path));
    }

    @Test
    public void testCommitNonExistentTX() throws Exception {
        assertFalse(ctrl.commit(UUID.randomUUID().toString()));
    }

    @Test
    public void testCancelNonExistentTX() throws Exception {
        assertFalse(ctrl.cancel(UUID.randomUUID().toString()));
    }

    /**
     * This is quite complex test scenario. First, lets create
     * {@link NetworkTopology} with some node in it. Then, write it to
     * datastore. Lastly, use {@link RemoteControl} to read this data using
     * JSON-RPC request.
     */
    @Test
    public void testReadTopologyData() throws Exception {
        final BindingToNormalizedNodeCodec codec = NormalizedNodesHelper.getBindingToNormalizedNodeCodec();
        final Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> e = TestUtils.getMockTopologyAsDom(schemaContext);
        final DOMDataWriteTransaction wtx = getDomBroker().newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.OPERATIONAL, e.getKey(), e.getValue());
        wtx.submit().checkedGet();

        InstanceIdentifier<NetworkTopology> nii = InstanceIdentifier.create(NetworkTopology.class);

        YangInstanceIdentifier yii = codec.toYangInstanceIdentifier(nii);
        dumpYii(yii);

        final JsonElement path = conv.toBus(yii, null).getPath();
        final YangInstanceIdentifier parsedYii = ctrl.path2II(path);
        assertEquals(yii, parsedYii);

        LOG.info("JSON path : {}", path);
        LOG.info("path from codec : {}", yii);

        assertNotNull(ctrl.read(Util.store2int(LogicalDatastoreType.OPERATIONAL), "test-model", path));
    }

    /**
     * Test path to leaf in container.
     */
    @Test
    public void testPathToLeaf() {
        final YangInstanceIdentifier yii = YangInstanceIdentifier.builder().node(NetworkTopology.QNAME)
                .node(Topology.QNAME)
                .nodeWithKey(Topology.QNAME, QName.create(Topology.QNAME, "topology-id"), "topology1")
                .node(QName.create(Topology.QNAME, "server-provided")).build();
        LOG.info("YII : {}", yii);
        final YangInstanceIdentifier ii = ctrl.path2II(parser.parse("{\"network-topology:network-topology\": "
                + "{\"topology\": [{\"topology-id\": \"topology1\",\"server-provided\": {}}]}}"));
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
        final YangInstanceIdentifier ii = ctrl.path2II(parser.parse(TOPO_TP_DATA));
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
            "null",
            "ABCD",
            "[]",
            "{\"non-existent-module:data1\":{}}"
        };

        //@formatter:off
        for (final String p : paths) {
            try {
                YangInstanceIdentifierDeserializer.toYangInstanceIdentifier(parser.parse(p), schemaContext);
                fail("This path should not be parseable !  : " + p);
            } catch (RuntimeException e) {
                LOG.info("This was expected : " + e.getMessage());
            }
        }
    }

    @Test
    public void testTxCancel() {
        UUID uuid = UUID.fromString(ctrl.txid());
        assertTrue(ctrl.cancel(uuid.toString()));
        assertFalse(ctrl.cancel(uuid.toString()));
    }

    /**
     * This test doesn't test anything, it is used as reference for other tests (merging related).
     */
    @Test
    public void testMerge() throws OperationFailedException {
        final BindingToNormalizedNodeCodec codec = NormalizedNodesHelper.getBindingToNormalizedNodeCodec();
        DOMDataWriteTransaction wtx = getDomBroker().newWriteOnlyTransaction();
        Config c1 = new ConfigBuilder().setWhoAmI(new Uri("urn:bla"))
                .setConfiguredEndpoints(Lists.<ConfiguredEndpoints>newArrayList()).build();

        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> e1 = codec
                .toNormalizedNode(InstanceIdentifier.create(Config.class), c1);

        wtx.put(LogicalDatastoreType.CONFIGURATION, e1.getKey(), e1.getValue());
        wtx.submit().checkedGet();
        ConfiguredEndpoints c2 = new ConfiguredEndpointsBuilder().setName("name-1")
                .setModules(Lists.newArrayList(new YangIdentifier("ietf-inet-types"))).build();

        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> e2 = codec.toNormalizedNode(InstanceIdentifier
                .builder(Config.class).child(ConfiguredEndpoints.class, new ConfiguredEndpointsKey("name-1")).build(),
                c2);

        wtx = getDomBroker().newWriteOnlyTransaction();
        LOG.info("Merging data {} at path {}", e2.getValue(), e2.getKey());
        wtx.merge(LogicalDatastoreType.CONFIGURATION, e2.getKey(), e2.getValue());
        wtx.submit().checkedGet();
    }

    @Test
    public void testReducedData() {
        YangInstanceIdentifier yii = YangInstanceIdentifierDeserializer.toYangInstanceIdentifier(
                parser.parse(MLX_JSON_PATH),
                schemaContext);
        assertNotNull(conv.jsonElementToNormalizedNode(parser.parse(MLX_CONFIG_DATA), yii, true));
    }

    /**
     * Here list item data are merged into non-existent list.
     */
    @Test
    public void testMergeListItemNonExistentList() throws InterruptedException {
        String txId = ctrl.txid();
        ctrl.merge(txId, Util.store2int(LogicalDatastoreType.CONFIGURATION), "",
                parser.parse(MLX_JSON_PATH),
                parser.parse(MLX_CONFIG_DATA));
        assertTrue(ctrl.commit(txId));
    }

    @Test
    public void testFailedTransactionsVanished() throws InterruptedException {
        String txId = ctrl.txid();
        ctrl.delete(txId, Util.store2int(LogicalDatastoreType.CONFIGURATION), "test-model", parser.parse("{}"));
        assertFalse(ctrl.commit(txId));
        List<String> err = ctrl.error(txId);
        assertFalse(err.isEmpty());
        LOG.info("Collected errors : {}", err);
        retryAction(TimeUnit.SECONDS, 5, () -> ctrl.isTxMapEmpty());
    }

    @Test
    public void testTxPutMergeDelete() throws Exception {
        //@formatter:off
        UUID uuid = UUID.fromString(ctrl.txid());

        ctrl.put(uuid.toString(),
                Util.store2int(LogicalDatastoreType.OPERATIONAL),
                "test-model", // entity
                parser.parse(TEST_MODEL_PATH), // path
                parser.parse("{\"test-model:top-element\":{\"level2a\":{}}}")); // data

        ctrl.commit(uuid.toString());

        uuid = UUID.randomUUID();
        ctrl.put(uuid.toString(),
                Util.store2int(LogicalDatastoreType.CONFIGURATION),
                "something",
                parser.parse("{\"jsonrpc:config\":{}}"),
                parser.parse("{\"jsonrpc:config\":{\"configured-endpoints\":[]}}"));
        ctrl.commit(uuid.toString());

        uuid = UUID.randomUUID();
        ctrl.merge(uuid.toString(),
                Util.store2str(Util.store2int(LogicalDatastoreType.CONFIGURATION)),
                "something",
                parser.parse(MLX_JSON_PATH),
                parser.parse(MLX_CONFIG_DATA));
        ctrl.commit(uuid.toString());

        assertTrue(ctrl.exists(Util.store2int(LogicalDatastoreType.OPERATIONAL),
                "test-model", // entity
                parser.parse(TEST_MODEL_PATH))); // path

        uuid = UUID.randomUUID();
        ctrl.delete(uuid.toString(),
                Util.store2int(LogicalDatastoreType.OPERATIONAL),
                "test-model", // entity
                parser.parse(TEST_MODEL_PATH)); // path

        ctrl.commit(uuid.toString());

        assertTrue(ctrl.error(uuid.toString()).isEmpty());

        assertFalse(ctrl.exists(Util.store2int(LogicalDatastoreType.OPERATIONAL),
                "test-model", // entity
                parser.parse(TEST_MODEL_PATH) // path
                ));
        //@formatter:on
    }

    @Test
    public void testPutWithoutTx() {
        ctrl.put(UUID.randomUUID().toString(), Util.store2int(LogicalDatastoreType.OPERATIONAL), // entity
                "test-model", parser.parse(TEST_MODEL_PATH), // path
                parser.parse("{ \"test-model:top-element\" : { \"level2a\" : {}}}") // data
        );
    }

    @Test
    public void testPutReducedDataForm() {
        final String uuid = UUID.randomUUID().toString();
        ctrl.put(uuid, Util.store2int(LogicalDatastoreType.CONFIGURATION), "test-model",
                parser.parse("{\"test-model:grillconf\":{}}"), parser.parse("{\"gasKnob\":10}"));
        assertTrue(ctrl.commit(uuid));
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
