/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import com.google.common.base.Preconditions;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer.NodeResult;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;

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
            .setTopology(BindingMap.of(
                    new TopologyBuilder()
                        .setServerProvided(true)
                        .setTopologyId(new TopologyId("topo1"))
                        .setNode(BindingMap.of(
                            new NodeBuilder()
                                .setNodeId(new NodeId("node1"))
                                .setTerminationPoint(BindingMap.of(
                                        new TerminationPointBuilder()
                                        .withKey(new TerminationPointKey(new TpId("eth0")))
                                        .build()
                                        ))
                                .build()
                            ))
                        .build()
                    ))
            .build();
        //@formatter:on
    }

    public static NodeResult getMockTopologyAsDom(BindingNormalizedNodeSerializer codec) {
        final NetworkTopology nt = getMockTopology();
        return codec.toNormalizedDataObject(InstanceIdentifier.create(NetworkTopology.class), nt);
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
