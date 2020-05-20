/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.remove;

import akka.actor.ActorRef;
import akka.cluster.Cluster;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.jsonrpc.provider.cluster.impl.ClusterDependencies;
import org.opendaylight.jsonrpc.provider.cluster.impl.RemotePeerActor;
import org.opendaylight.jsonrpc.provider.common.MappedPeerContext;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MasterProvider implements AutoCloseable {
    private ClusterDependencies dependencies;
    private final Map<String, ClusterSingletonServiceRegistration> map = new ConcurrentHashMap<>();
    private ActorRef masterActorRef;

    public MasterProvider(Peer peer, ClusterDependencies dependencies) {

    }

    @Override
    public void close() {

    }

    public void enable() {
        final String masterAddress = Cluster.get(dependencies.getActorSystem()).selfAddress().toString();
        masterActorRef = dependencies.getActorSystem()
                .actorOf(RemotePeerActor.props(null, null, dependencies), masterAddress);

    }

    public void disable() {

    }

    private void connectPeer(InstanceIdentifier<Peer> ii, Peer peer) {
        try {
            final MappedPeerContext ctx = new MappedPeerContext(peer, dependencies.getTransportFactory(),
                    dependencies.getSchemaService(), dependencies.getDataBroker(),
                    dependencies.getDomMountPointService(), null, dependencies.getYangXPathParserFactory());

            final ClusterSingletonServiceRegistration registration = dependencies.getClusterSingletonServiceProvider()
                    .registerClusterSingletonService(new ClusteredPeerContext(peer, dependencies));

        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

}
