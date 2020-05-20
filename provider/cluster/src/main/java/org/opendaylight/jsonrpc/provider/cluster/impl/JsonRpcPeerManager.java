/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.impl;

import static org.opendaylight.jsonrpc.provider.cluster.impl.ClusterUtil.createActorPath;
import static org.opendaylight.jsonrpc.provider.cluster.impl.ClusterUtil.createMasterActorName;
import static org.opendaylight.jsonrpc.provider.cluster.impl.ClusterUtil.getPeerOpstateIdentifier;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.PoisonPill;
import akka.dispatch.OnComplete;
import akka.pattern.AskTimeoutException;
import akka.pattern.Patterns;
import akka.util.Timeout;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.jsonrpc.provider.cluster.api.JsonRpcPeerSingletonService;
import org.opendaylight.jsonrpc.provider.cluster.messages.AskForMasterMountPoint;
import org.opendaylight.jsonrpc.provider.cluster.messages.UnregisterMountPoint;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.MountStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpoints;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager of {@link Peer}'s state in clustered environment. Takes care of creating mountpoint on slave node based on
 * change in {@link Peer}'s operational status.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 1, 2020
 */
public class JsonRpcPeerManager
        implements ClusteredDataTreeChangeListener<ActualEndpoints>, JsonRpcPeerSingletonService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcPeerManager.class);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final ListenerRegistration<JsonRpcPeerManager> dtclRegistration;
    private final Peer peer;
    private final ClusterDependencies dependencies;
    private final Timeout askTimeout;
    private final AtomicInteger updateCounter = new AtomicInteger();

    @GuardedBy("this")
    private ActorRef slaveActorRef;

    public JsonRpcPeerManager(@NonNull ClusterDependencies dependencies, @NonNull Peer peer,
            @NonNull Timeout askTimeout) {
        this.dependencies = Objects.requireNonNull(dependencies);
        this.peer = Objects.requireNonNull(peer);
        dtclRegistration = dependencies.getDataBroker()
                .registerDataTreeChangeListener(getPeerOpstateIdentifier(peer.getName()), this);
        this.askTimeout = Objects.requireNonNull(askTimeout);
    }

    @Override
    public void onDataTreeChanged(@NonNull Collection<DataTreeModification<ActualEndpoints>> changes) {
        for (final DataTreeModification<ActualEndpoints> change : changes) {
            final DataObjectModification<ActualEndpoints> rootNode = change.getRootNode();
            LOG.debug("[{}]: DTC {} => {}", peer.getName(), rootNode.getDataBefore(), rootNode.getDataAfter());
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                    peerStateChanged(rootNode.getDataAfter());
                    break;
                case WRITE:
                    peerStateChanged(rootNode.getDataAfter());
                    break;
                case DELETE:
                    destroyMountpoint();
                    break;
                default:
                    LOG.debug("[{}]: Unhandled modification: {}", peer.getName(), rootNode.getModificationType());
            }
        }
    }

    private synchronized void destroyMountpoint() {
        updateCounter.incrementAndGet();
        if (slaveActorRef != null) {
            LOG.debug("[{}] : Sending UnregisterSlaveMountPoint to {}", peer.getName(), slaveActorRef);
            slaveActorRef.tell(UnregisterMountPoint.INSTANCE, ActorRef.noSender());
        }
    }

    private void peerStateChanged(@Nullable ActualEndpoints node) {
        if (closed.get()) {
            return;
        }
        if (MountStatus.Mounted.equals(node.getMountStatus())) {
            updateCounter.incrementAndGet();
            ensureSlaveActor();
            final String masterAddress = node.getManagedBy();
            final String masterActorPath = createActorPath(masterAddress,
                    createMasterActorName(peer.getName(), masterAddress));

            final AskForMasterMountPoint msg = new AskForMasterMountPoint(slaveActorRef);
            final ActorSelection masterActor = dependencies.getActorSystem().actorSelection(masterActorPath);

            LOG.debug("[{}]: Sending {} to master {}", peer.getName(), msg, masterActor);
            askForMountPoint(msg, masterActor, 1, updateCounter.get());
        } else {
            destroyMountpoint();
        }
    }

    @Holding("this")
    private void askForMountPoint(final AskForMasterMountPoint msg, final ActorSelection masterActor, final int tries,
            final int updateCount) {

        Patterns.ask(masterActor, msg, askTimeout).onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object response) {
                synchronized (this) {
                    // Ignore the response if we were since closed or another notification update occurred.
                    if (closed.get() || updateCount != updateCounter.intValue()) {
                        return;
                    }

                    if (failure instanceof AskTimeoutException) {
                        if (tries <= 5 || tries % 10 == 0) {
                            LOG.warn("[{}]: Failed to send message to {} - retrying...", peer.getName(), masterActor,
                                    failure);
                        }
                        askForMountPoint(msg, masterActor, tries + 1, updateCount);
                    } else if (failure != null) {
                        LOG.error("[{}]: Failed to send message {} to {}. Slave mount point could not be created",
                                peer.getName(), msg, masterActor, failure);
                    } else {
                        LOG.debug("[{}]: {} message to {} succeeded", peer.getName(), msg, masterActor);
                    }
                }
            }
        }, dependencies.getActorSystem().dispatcher());
    }

    private void ensureSlaveActor() {
        if (slaveActorRef == null) {
            slaveActorRef = dependencies.getActorSystem()
                    .actorOf(RemotePeerActor.props(peer, askTimeout, dependencies));
            LOG.debug("[{}]:Created slave actor : {}", peer.getName(), slaveActorRef);
        }
    }

    @Override
    public void close() throws Exception {
        if (!closed.compareAndSet(false, true)) {
            LOG.info("[{}] : closing", peer.getName());
            dtclRegistration.close();
            if (slaveActorRef != null) {
                slaveActorRef.tell(PoisonPill.getInstance(), ActorRef.noSender());
                slaveActorRef = null;
            }
        }
    }
}
