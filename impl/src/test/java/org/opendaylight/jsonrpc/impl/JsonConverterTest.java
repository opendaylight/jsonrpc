/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.StringWriter;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.model.JSONRPCArg;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
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
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link JsonConverter}.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class JsonConverterTest extends AbstractDataBrokerTest {
    private static final Logger LOG = LoggerFactory.getLogger(JsonConverter.class);
    private JsonConverter conv;
    private SchemaContext schemaContext;

    @Override
    protected void setupWithSchema(SchemaContext context) {
        this.schemaContext = context;
        super.setupWithSchema(context);
    }

    @Before
    public void setUp() {
        NormalizedNodesHelper.init(schemaContext);
        conv = new JsonConverter(schemaContext);
    }

    @After
    public void tearDown() {
        LOG.info(Strings.repeat("-", 120));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConvertContainerNode() throws IOException {
        StringWriter sw = new StringWriter();
        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> data = createContainerNodeData();
        dump((NormalizedNode<PathArgument, ?>) data.getValue(), sw, 1);
        LOG.info("Normalized node content : \n{}", sw.toString());
        JSONRPCArg arg = conv.convert(data.getKey(), data.getValue());
        LOG.info("Path : '{}' Data : '{}'", arg.path, arg.data);
        assertThat(arg.data.toString(), hasJsonPath("$.topology[0].topology-id", equalTo("topo-id")));
        assertThat(arg.data.toString(), hasJsonPath("$.topology[0].node", hasSize(2)));
        assertThat(arg.path.toString(), hasJsonPath("$.network-topology:network-topology.*", hasSize(0)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConvertLeafNode() throws IOException {
        StringWriter sw = new StringWriter();
        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> data = createLeafNodeData();
        dump((NormalizedNode<PathArgument, ?>) data.getValue(), sw, 1);
        LOG.info("Normalized node content : \n{}", sw.toString());
        JSONRPCArg arg = conv.convert(data.getKey(), data.getValue());
        LOG.info("Path : '{}' Data : '{}'", arg.path, arg.data);
        assertThat(arg.path.toString(), hasJsonPath("$..topology[0].topology-id", contains("topo-id")));
    }

    @Test
    public void testConvertListNode() throws IOException {
        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> data = createListNodeData();
        JSONRPCArg arg = conv.convert(data.getKey(), data.getValue());
        LOG.info("Path : '{}' Data : '{}'", arg.path, arg.data);
        assertThat(arg.path.toString(), hasJsonPath("$..topology.topology", hasSize(1)));
    }

    @Test
    public void testConvertEmptyPath() throws IOException {
        JSONRPCArg arg = conv.convert(YangInstanceIdentifier.EMPTY,
                Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(NetworkTopology.QNAME)).build());
        LOG.info("Path : '{}' Data : '{}'", arg.path, arg.data);
        assertNull(arg.data);
        assertNull(arg.path);
    }

    @Test
    public void testConvertNullData() throws IOException {
        JSONRPCArg arg = conv.convert(YangInstanceIdentifier.of(NetworkTopology.QNAME), null);
        LOG.info("Path : '{}' Data : '{}'", arg.path, arg.data);
        assertNull(arg.data);
        JsonObject obj = new JsonObject();
        obj.add(NetworkTopology.QNAME.getLocalName() + ':' + NetworkTopology.QNAME.getLocalName(), new JsonObject());
        assertEquals(obj, arg.path);
    }

    @Test
    public void testConvertRpcData() throws IOException {
        JsonObject obj = conv.rpcConvert(SchemaPath.create(true), ImmutableNodes.containerNode(NetworkTopology.QNAME));
        LOG.info("JSON object : '{}'", obj);
        assertNotNull(obj);
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
        final List<Node> data = Lists.newArrayList(new NodeBuilder().setNodeId(new NodeId("node-id-1")).build(),
                new NodeBuilder()
                        .setTerminationPoint(
                                Lists.newArrayList(new TerminationPointBuilder().setTpId(new TpId("eth0")).build()))
                        .setNodeId(new NodeId("node-id-2")).build());
        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> data2 = NormalizedNodesHelper
                .getBindingToNormalizedNodeCodec()
                .toNormalizedNode(InstanceIdentifier.create(NetworkTopology.class).child(Topology.class),
                        new TopologyBuilder().setTopologyId(new TopologyId("topo-id")).setNode(data).build());
        NormalizedNode<?, ?> sub1 = data2.getValue();
        return new SimpleEntry<>(data2.getKey(), sub1);
    }

    private Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> createLeafNodeData() {
        final QName topoIdQname = QName.create(NetworkTopology.QNAME, "topology-id");
        final InstanceIdentifier<Topology> ii = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class,
                new TopologyKey(new TopologyId("topo-id")));
        LOG.info("II : {}, YII : {}", ii,
                NormalizedNodesHelper.getBindingToNormalizedNodeCodec().toYangInstanceIdentifier(ii));
        return new SimpleEntry<>(
                NormalizedNodesHelper.getBindingToNormalizedNodeCodec().toYangInstanceIdentifier(ii),
                ImmutableNodes.leafNode(topoIdQname, "topo-id"));
    }

    static Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> createContainerNodeData() {
        //@formatter:off
        final InstanceIdentifier<NetworkTopology> ii = InstanceIdentifier.create(NetworkTopology.class);
        final NetworkTopology dObj = new NetworkTopologyBuilder()
                .setTopology(Lists.newArrayList(new TopologyBuilder()
                        .setNode(Lists.newArrayList(
                                new NodeBuilder()
                                    .setNodeId(new NodeId("node-id-1"))
                                .build(),
                                new NodeBuilder()
                                    .setTerminationPoint(Lists.newArrayList(
                                            new TerminationPointBuilder()
                                                .setTpId(new TpId("eth0"))
                                            .build()
                                            ))
                                    .setNodeId(new NodeId("node-id-2"))
                            .build()
                                ))
                        .setTopologyId(new TopologyId("topo-id"))
                        .build()))
                .build();
        return NormalizedNodesHelper.getBindingToNormalizedNodeCodec().toNormalizedNode(ii, dObj);
        //@formatter:on
    }
}
