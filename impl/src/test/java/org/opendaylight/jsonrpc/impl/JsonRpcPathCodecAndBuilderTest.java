/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static org.junit.Assert.assertEquals;

import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRpcPathCodecAndBuilderTest extends AbstractJsonRpcTest {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcPathCodecAndBuilderTest.class);
    private static final JsonObject CE_PATH = JsonRpcPathBuilder.newBuilder()
            .container("jsonrpc:config")
            .container("configured-endpoints")
            .item("name", "endpoint-1")
            .build();
    private JsonRpcPathCodec codec;

    @Before
    public void setUp() {
        codec = JsonRpcPathCodec.create(schemaContext);
    }

    @Test
    public void testDecode() {
        YangInstanceIdentifier yii = Util.createBiPath("endpoint-1");
        final JsonObject serialized = codec.serialize(yii);
        assertEquals(CE_PATH, serialized);
    }

    @Test
    public void testEncodeSimpleKey() {
        JsonObject jsonPath = JsonRpcPathBuilder.newBuilder()
                .container("jsonrpc:config")
                .container("configured-endpoints")
                .item("name", "endpoint-1")
                .build();
        YangInstanceIdentifier yii = codec.deserialize(jsonPath);
        LOG.info("Result : {}", yii);

        JsonObject encoded = codec.serialize(yii);

        LOG.info("Result : {}", encoded);

        assertEquals(jsonPath, encoded);
    }

    @Test
    public void testEncodeMultilevelPath() {

        InstanceIdentifier<?> ii = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId("topo-1")))
                .child(Node.class, new NodeKey(new NodeId("node-1")))
                .child(TerminationPoint.class, new TerminationPointKey(new TpId("eth0")));
        YangInstanceIdentifier yiiExpected = getCodec().toYangInstanceIdentifier(ii);
        LOG.info("Expected : {}", yiiExpected);

        JsonConverter jsonConverter = new JsonConverter(schemaContext);
        LOG.info("{}", jsonConverter.toBus(yiiExpected, null).getPath());

        JsonObject jsonPath = JsonRpcPathBuilder.newBuilder("network-topology:network-topology")
                .container("topology")
                .item("topology-id", "topo-1")
                .container("node")
                .item("node-id", "node-1")
                .container("termination-point")
                .item("tp-id", "eth0")
                .build();

        LOG.info("IN:{}", jsonPath);
        YangInstanceIdentifier yii = codec.deserialize(jsonPath);
        LOG.info("Result : {}", yii);
        assertEquals(yiiExpected, yii);
    }
}
