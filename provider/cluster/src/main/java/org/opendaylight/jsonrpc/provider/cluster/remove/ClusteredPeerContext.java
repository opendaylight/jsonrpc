/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.remove;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.util.Timeout;
import com.google.common.util.concurrent.ListenableFuture;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.provider.cluster.impl.RemotePeerActor;
import org.opendaylight.jsonrpc.provider.common.AbstractPeerContext;
import org.opendaylight.jsonrpc.provider.common.FailedPeerContext;
import org.opendaylight.jsonrpc.provider.common.MappedPeerContext;
import org.opendaylight.jsonrpc.provider.common.ProviderDependencies;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusteredPeerContext implements ClusterSingletonService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ClusteredPeerContext.class);
    private final ServiceGroupIdentifier sid;
    private final AtomicBoolean master = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private ActorSystem actorSystem;
    private Peer peer;
    private DOMMountPointService domMountPointService;
    private Timeout timeout;
    private ActorRef masterActorRef;
    private AbstractPeerContext ctx;
    private ProviderDependencies dependencies;

    public ClusteredPeerContext(Peer peer, ProviderDependencies dependencies) {
        this.sid = ServiceGroupIdentifier.create("jsonrpc/peer/" + peer.getName());
        this.dependencies = dependencies;
    }

    @Override
    public @NonNull ServiceGroupIdentifier getIdentifier() {
        return sid;
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Master elected {}", sid);
        if (!closed.get()) {
            final String masterAddress = Cluster.get(actorSystem).selfAddress().toString();
            masterActorRef = actorSystem.actorOf(RemotePeerActor.props(peer, timeout, dependencies),
                    masterAddress);
            try {
                ctx = new MappedPeerContext(peer, dependencies.getTransportFactory(), dependencies.getSchemaService(),
                        dependencies.getDataBroker(), dependencies.getDomMountPointService(), null /* TODO */,
                        dependencies.getYangXPathParserFactory());
            } catch (URISyntaxException e) {
                ctx = new FailedPeerContext(peer, dependencies.getDataBroker(), e);
                LOG.warn("Unable to mount '{}'", peer.getName(), e);
            }

        }
        master.set(true);
    }

    @Override
    public ListenableFuture<? extends Object> closeServiceInstance() {
        master.set(false);
        return FluentFutures.immediateNullFluentFuture();
    }

    @Override
    public void close() throws Exception {
        if (closed.compareAndSet(false, true)) {
            Util.closeAndLogOnError(ctx);
            // TODO : disable slave setup
            actorSystem.stop(masterActorRef);
        }
    }
}
