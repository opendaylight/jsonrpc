/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.StringWriter;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Map.Entry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.impl.JsonConverter;
import org.opendaylight.jsonrpc.impl.JsonRpcPathCodec;
import org.opendaylight.jsonrpc.model.JSONRPCArg;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.Ipv4Key;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link JsonConverter}.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public class JsonConverterTest extends AbstractJsonRpcTest {
    private static final Logger LOG = LoggerFactory.getLogger(JsonConverter.class);
    private JsonConverter conv;
    private JsonRpcPathCodec pathCodec;

    @Before
    public void setUp() {
        conv = new JsonConverter(schemaContext);
        pathCodec = JsonRpcPathCodec.create(schemaContext);
    }

    @After
    public void tearDown() {
        LOG.info(Strings.repeat("-", 120));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConvertContainerNode() throws IOException {
        StringWriter sw = new StringWriter();
        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> data = createContainerNodeData(getCodec());
        dump((NormalizedNode<PathArgument, ?>) data.getValue(), sw, 1);
        LOG.info("Normalized node content : \n{}", sw.toString());
        JSONRPCArg arg = conv.toBus(data.getKey(), data.getValue());
        LOG.info("Path : '{}' Data : '{}'", arg.getPath(), arg.getData());
        assertThat(arg.getData().toString(), hasJsonPath("$.topology[0].topology-id", equalTo("topo-id")));
        assertThat(arg.getData().toString(), hasJsonPath("$.topology[0].node", hasSize(2)));
        assertThat(arg.getPath().toString(), hasJsonPath("$.network-topology:network-topology.*", hasSize(0)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConvertLeafNode() throws IOException {
        StringWriter sw = new StringWriter();
        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> data = createLeafNodeData();
        dump((NormalizedNode<PathArgument, ?>) data.getValue(), sw, 1);
        LOG.info("Normalized node content : \n{}", sw.toString());
        JSONRPCArg arg = conv.toBus(data.getKey(), data.getValue());
        LOG.info("Path : '{}' Data : '{}'", arg.getPath(), arg.getData());
        assertThat(arg.getPath().toString(), hasJsonPath("$..topology[0].topology-id", contains("topo-id")));
        // This doubles up as a test for the JSONRPC-9 regression
        assertEquals("{\"network-topology:network-topology\":{\"topology\":[{\"topology-id\":\"topo-id\"}]}}",
                arg.getPath().toString());
    }

    @Test
    public void testConvertListNode() throws IOException {
        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> data = createListNodeData();
        JSONRPCArg arg = conv.toBus(data.getKey(), data.getValue());
        LOG.info("Path : '{}' Data : '{}'", arg.getPath(), arg.getData());
        assertThat(arg.getPath().toString(), hasJsonPath("$..topology.topology", hasSize(1)));
    }

    @Test
    public void testConvertEmptyPath() throws IOException {
        JSONRPCArg arg = conv.toBus(YangInstanceIdentifier.empty(),
                Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(NetworkTopology.QNAME)).build());
        LOG.info("Path : '{}' Data : '{}'", arg.getPath(), arg.getData());
        assertNull(arg.getData());
        assertNull(arg.getPath());
    }

    @Test
    public void testConvertNullData() throws IOException {
        JSONRPCArg arg = conv.toBus(YangInstanceIdentifier.of(NetworkTopology.QNAME), null);
        LOG.info("Path : '{}' Data : '{}'", arg.getPath(), arg.getData());
        assertNull(arg.getData());
        JsonObject obj = new JsonObject();
        obj.add(NetworkTopology.QNAME.getLocalName() + ':' + NetworkTopology.QNAME.getLocalName(), new JsonObject());
        assertEquals(obj, arg.getPath());
    }

    @Test
    public void testConvertRpcData() throws IOException {
        JsonObject obj = conv.rpcConvert(SchemaPath.create(true), ImmutableNodes.containerNode(NetworkTopology.QNAME));
        LOG.info("JSON object : '{}'", obj);
        assertNotNull(obj);
    }


    /**
     * Unit test for <a href="https://jira.opendaylight.org/browse/JSONRPC-9">JSONRPC-9 bug</a>.
     */
    @Test
    public void testTopLevelListDeserialization() {
        JsonElement path = jsonParser.parse("{\"test-model:ipv4\":[{\"name\":\"eth0\"}]}");
        YangInstanceIdentifier yii = pathCodec.deserialize(path.getAsJsonObject());
        LOG.info("{}", yii);
        assertNotNull(yii);
        path = jsonParser.parse("{\"test-model:ipv4\":{\"ipv4\":[{\"name\":\"eth0\"}]}}");
        yii = pathCodec.deserialize(path.getAsJsonObject());
        LOG.info("{}", yii);
        assertNotNull(yii);
    }

    @Test
    public void testTopListSerialization() {
        Ipv4 ipv4 = new Ipv4Builder().withKey(new Ipv4Key("eth0"))
                .build();
        InstanceIdentifier<Ipv4> path = InstanceIdentifier.builder(Ipv4.class, new Ipv4Key("eth0")).build();
        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalized = getCodec().toNormalizedNode(path, ipv4);
        JSONRPCArg arg = conv.toBus(normalized.getKey(), normalized.getValue());
        LOG.info("Result : {}", arg.getPath());
        assertEquals("{\"test-model:ipv4\":[{\"name\":\"eth0\"}]}",arg.getPath().toString());

    }

    @SuppressWarnings("unchecked")
    private void dump(NormalizedNode<PathArgument, ?> nn, StringWriter sw, int level) {
        sw.write(Strings.repeat(" ", (level - 1) * 2));
        sw.write(nn.getValue().getClass().getSimpleName());
        sw.write(" : ");
        sw.write(nn.getIdentifier().toString());
        sw.write("\n");
        if (nn.getValue() instanceof Collection) {
            for (Object e : (Collection<?>) nn.getValue()) {
                if (e instanceof NormalizedNode) {
                    dump((NormalizedNode<PathArgument, ?>) e, sw, level + 1);
                }
            }
        }
    }

    private Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> createListNodeData() {
        final Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> data2 = getCodec()
                .toNormalizedNode(InstanceIdentifier.create(NetworkTopology.class).child(Topology.class),
                        new TopologyBuilder().setTopologyId(new TopologyId("topo-id"))
                                .setNode(compatMap(Lists.newArrayList(
                                        new NodeBuilder().setNodeId(new NodeId("node-id-1")).build(),
                                        new NodeBuilder().setTerminationPoint(compatItem(
                                                new TerminationPointBuilder().setTpId(new TpId("eth0")).build()))
                                                .setNodeId(new NodeId("node-id-2"))
                                                .build())))
                                .build());
        NormalizedNode<?, ?> sub1 = data2.getValue();
        return new SimpleEntry<>(data2.getKey(), sub1);
    }

    private Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> createLeafNodeData() {
        final QName topoIdQname = QName.create(NetworkTopology.QNAME, "topology-id");
        final InstanceIdentifier<Topology> ii = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId("topo-id")));
        LOG.info("II : {}, YII : {}", ii, getCodec().toYangInstanceIdentifier(ii));
        return new SimpleEntry<>(getCodec().toYangInstanceIdentifier(ii),
                ImmutableNodes.leafNode(topoIdQname, "topo-id"));
    }

    static Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> createContainerNodeData(
            BindingNormalizedNodeSerializer codec) {
        //@formatter:off
        final InstanceIdentifier<NetworkTopology> ii = InstanceIdentifier.create(NetworkTopology.class);
        final NetworkTopology dObj = new NetworkTopologyBuilder()
                .setTopology(compatItem(new TopologyBuilder()
                        .setNode(compatMap(Lists.newArrayList(
                                new NodeBuilder()
                                .setNodeId(new NodeId("node-id-1"))
                            .build(),
                            new NodeBuilder()
                                .setTerminationPoint(compatItem(new TerminationPointBuilder()
                                            .setTpId(new TpId("eth0"))
                                        .build()))
                                .setNodeId(new NodeId("node-id-2"))
                        .build()
                            )))
                        .setTopologyId(new TopologyId("topo-id"))
                        .build()))
                .build();
        return codec.toNormalizedNode(ii, dObj);
        //@formatter:on
    }
}
