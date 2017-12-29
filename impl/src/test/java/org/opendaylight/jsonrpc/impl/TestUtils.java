/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Utility class to reduce code duplication.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public final class TestUtils {
    public static final String MOCK_TOPO_TP_ID_YII_PATH = "{\"network-topology:network-topology\":"
            + "{\"topology\":[{\"topology-id\":\"topology1\",\"node\":[{\"node-id\":\"node1\","
            + "\"termination-point\":[{\"tp-id\": \"eth0\"}]}]}]}}";

    private TestUtils() {
    }

    public static NetworkTopology getMockTopology() {
        //@formatter:off
        return new NetworkTopologyBuilder()
            .setTopology(Lists.<Topology>newArrayList(
                    new TopologyBuilder()
                        .setServerProvided(true)
                        .setTopologyId(new TopologyId("topo1"))
                        .setNode(Lists.<Node>newArrayList(
                            new NodeBuilder()
                                .setNodeId(new NodeId("node1"))
                                .setTerminationPoint(Lists.<TerminationPoint>newArrayList(
                                        new TerminationPointBuilder()
                                        .setKey(new TerminationPointKey(new TpId("eth0")))
                                        .build()
                                        ))
                                .build()
                            ))
                        .build()
                    ))
            .build();
        //@formatter:on
    }

    public static Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> getMockTopologyAsDom(
            SchemaContext schemaContext) {
        final NetworkTopology nt = getMockTopology();
        NormalizedNodesHelper.init(schemaContext);
        final BindingToNormalizedNodeCodec codec = NormalizedNodesHelper.getBindingToNormalizedNodeCodec();
        return codec.toNormalizedNode(InstanceIdentifier.create(NetworkTopology.class), nt);
    }

    /**
     * Ensure that given class has only one private constructor.
     */
    public static boolean assertPrivateConstructor(Class<?> clazz) {
        final Constructor<?>[] ctors = clazz.getDeclaredConstructors();
        Preconditions.checkArgument(ctors.length == 1);
        try {
            ctors[0].newInstance();
        } catch (InstantiationException | IllegalArgumentException | InvocationTargetException e) {
            // no-op, ignore
        } catch (IllegalAccessException e) {
            return true;
        }
        return false;
    }
}
