/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.impl;

import static akka.pattern.Patterns.ask;
import static org.opendaylight.jsonrpc.provider.cluster.impl.ClusterUtil.DEFAULT_ASK_TIMEOUT;
import static org.opendaylight.jsonrpc.provider.cluster.impl.ClusterUtil.createActorPath;
import static org.opendaylight.jsonrpc.provider.cluster.impl.ClusterUtil.createMasterActorName;
import static org.opendaylight.jsonrpc.provider.cluster.impl.ClusterUtil.durationFromUint16seconds;
import static org.opendaylight.jsonrpc.provider.cluster.impl.ClusterUtil.getPeerOpstateIdentifier;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.PoisonPill;
import akka.dispatch.OnComplete;
import akka.util.Timeout;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.jsonrpc.provider.cluster.api.JsonRpcPeerSingletonService;
import org.opendaylight.jsonrpc.provider.cluster.messages.MountPointRequest;
import org.opendaylight.jsonrpc.provider.cluster.messages.UnregisterMountPoint;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.MountStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpoints;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

final class SlavePeerContext
        implements DataTreeChangeListener<ActualEndpoints>, JsonRpcPeerSingletonService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SlavePeerContext.class);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Registration dtclRegistration;
    private ActorRef slaveActorRef;
    private final Timeout askTimeout;
    private final Peer peer;
    private final ClusterDependencies dependencies;

    SlavePeerContext(Peer peer, ClusterDependencies dependencies) {
        this.peer = peer;
        this.dependencies = dependencies;
        dtclRegistration = dependencies.getDataBroker()
                .registerTreeChangeListener(getPeerOpstateIdentifier(peer.getName()), this);
        final Duration askDuration = dependencies.getConfig() == null ? DEFAULT_ASK_TIMEOUT
                : durationFromUint16seconds(dependencies.getConfig().getActorResponseWaitTime(), DEFAULT_ASK_TIMEOUT);
        askTimeout = Timeout.apply(askDuration.toSeconds(), TimeUnit.SECONDS);
        LOG.debug("[{}] Created {}", peer.getName(), this);
    }

    @Override
    public void onDataTreeChanged(List<DataTreeModification<ActualEndpoints>> changes) {
        for (final DataTreeModification<ActualEndpoints> change : changes) {
            final DataObjectModification<ActualEndpoints> rootNode = change.getRootNode();
            LOG.debug("[{}] OP DTC [{}] : {} => {}", peer.getName(), rootNode.modificationType(),
                    rootNode.dataBefore(), rootNode.dataAfter());
            switch (rootNode.modificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    onMountpointUpdated(rootNode.dataAfter());
                    break;
                case DELETE:
                    deleteMountpoint();
                    break;
                default:
                    LOG.warn("[{}] Unhandled modification {}", peer.getName(), rootNode.modificationType());
            }
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        LOG.info("[{}] Closing {}", peer.getName(), this);
        dtclRegistration.close();
        if (slaveActorRef != null) {
            LOG.debug("[{}] Sending poison pill to {}", peer.getName(), slaveActorRef);
            slaveActorRef.tell(PoisonPill.getInstance(), ActorRef.noSender());
            slaveActorRef = null;
        }
    }

    private synchronized void onMountpointUpdated(ActualEndpoints opState) {
        if (MountStatus.Mounted.equals(opState.getMountStatus())) {
            ensureSlaveActor();
            final String masterAddress = opState.getManagedBy();
            final String actorPath = createActorPath(masterAddress,
                    createMasterActorName(peer.getName(), masterAddress));
            final ActorSelection masterRef = dependencies.getActorSystem().actorSelection(actorPath);
            LOG.debug("Sending MountPointRequest to {}", masterRef);
            ask(masterRef, new MountPointRequest(slaveActorRef), askTimeout).onComplete(new OnComplete<>() {
                @Override
                public void onComplete(final Throwable failure, final Object response) {
                    final boolean failed = failure != null;
                    LOG.debug("[{}] MountPointRequest {}", peer.getName(), failed ? "failed" : "succeeded");
                    if (closed.get()) {
                        return;
                    }
                    if (failed) {
                        LOG.error("[{}] MountPointRequest failed", peer.getName(), failure);
                    }
                }
            }, dependencies.getActorSystem().dispatcher());
        } else {
            deleteMountpoint();
        }
    }

    private synchronized void deleteMountpoint() {
        if (slaveActorRef != null) {
            LOG.debug("[{}] Sending UnregisterMountPoint to {}", peer.getName(), slaveActorRef);
            slaveActorRef.tell(new UnregisterMountPoint(), ActorRef.noSender());
        }
    }

    private synchronized void ensureSlaveActor() {
        if (slaveActorRef == null) {
            slaveActorRef = dependencies.getActorSystem().actorOf(RemotePeerActor.props(peer, dependencies));
            LOG.debug("[{}] Slave actor created with name {}", peer.getName(), slaveActorRef);
        }
    }

    @Override
    public String toString() {
        return "SlavePeerContext [closed=" + closed + ", peer=" + peer.getName() + "]";
    }
}
