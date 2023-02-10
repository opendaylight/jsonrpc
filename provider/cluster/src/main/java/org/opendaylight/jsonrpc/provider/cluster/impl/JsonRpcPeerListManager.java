/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.impl;

import static org.opendaylight.jsonrpc.provider.common.Util.removeFromMapAndClose;

import akka.util.Timeout;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.provider.cluster.api.JsonRpcPeerSingletonService;
import org.opendaylight.jsonrpc.provider.common.AbstractPeerContext;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ForceRefreshInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ForceRefreshOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ForceRefreshOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ForceReloadInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ForceReloadOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ForceReloadOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.JsonrpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpoints;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that monitors list of {@link Peer}s in config DS and create/destroy mountpoints accordingly. Lifecycle of
 * this service is managed by blueprint.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 1, 2020
 */
@Component(service = { })
public final class JsonRpcPeerListManager implements ClusteredDataTreeChangeListener<ConfiguredEndpoints>,
        JsonRpcPeerSingletonService, JsonrpcService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcPeerListManager.class);
    private final ListenerRegistration<JsonRpcPeerListManager> dtcListener;
    @GuardedBy("this")
    private final Map<String, RemotePeerContext> peerMap = new HashMap<>();
    private final Map<String, ClusterSingletonServiceRegistration> clusterRegistrations = new HashMap<>();
    private final ClusterDependencies dependencies;

    public JsonRpcPeerListManager(ClusterDependencies dependencies) {
        this(dependencies, Timeout.apply(90, TimeUnit.SECONDS));
    }

    private JsonRpcPeerListManager(ClusterDependencies dependencies, Timeout askTimeout) {
        this.dependencies = dependencies;
        this.dtcListener = dependencies.getDataBroker()
                .registerDataTreeChangeListener(ClusterUtil.getPeerListIdentifier(), this);
    }

    @Override
    public void onDataTreeChanged(@NonNull Collection<DataTreeModification<ConfiguredEndpoints>> changes) {
        for (final DataTreeModification<ConfiguredEndpoints> change : changes) {
            final DataObjectModification<ConfiguredEndpoints> rootNode = change.getRootNode();
            final InstanceIdentifier<ConfiguredEndpoints> ident = change.getRootPath().getRootIdentifier();
            final String name = ClusterUtil.peerNameFromII(ident);
            LOG.debug("CFG DTC [{}] : {} => {}", rootNode.getModificationType(), rootNode.getDataBefore(),
                    rootNode.getDataAfter());
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                    updatePeerContext(rootNode.getDataAfter());
                    break;
                case WRITE:
                    if (peerMap.containsKey(name)) {
                        updatePeerContext(rootNode.getDataAfter());
                    } else {
                        createPeerContext(rootNode.getDataAfter());
                    }
                    break;
                case DELETE:
                    destroyPeerContext(name);
                    break;
                default:
                    LOG.warn("Unhandled data modification {}", rootNode.getModificationType());
            }
        }
    }

    @Holding("this")
    private synchronized void updatePeerContext(ConfiguredEndpoints peer) {
        if (peerMap.containsKey(peer.getName())) {
            destroyPeerContext(peer.getName());
            createPeerContext(peer);
        }
    }

    @Holding("this")
    private synchronized void destroyPeerContext(String name) {
        LOG.info("Removing context '{}'", name);
        removeFromMapAndClose(peerMap, name);
        removeFromMapAndClose(clusterRegistrations, name);
    }

    @Holding("this")
    private synchronized void createPeerContext(ConfiguredEndpoints peer) {
        LOG.info("Creating context for '{}'", peer.getName());
        final RemotePeerContext service = new RemotePeerContext(peer, dependencies);

        final ClusterSingletonServiceRegistration clusterContext = dependencies.getClusterSingletonServiceProvider()
                .registerClusterSingletonService(service);

        LOG.debug("Created {}", service);

        clusterRegistrations.put(peer.getName(), clusterContext);
        peerMap.put(peer.getName(), service);

    }

    @Override
    public void close() {
        dtcListener.close();
        clusterRegistrations.values().forEach(Util::closeAndLogOnError);
        peerMap.values().forEach(Util::closeAndLogOnError);
        peerMap.clear();
    }

    @Override
    public ListenableFuture<RpcResult<ForceRefreshOutput>> forceRefresh(ForceRefreshInput input) {
        // This is NOOP nowadays
        return Futures.immediateFuture(RpcResultBuilder.success(new ForceRefreshOutputBuilder().build()).build());
    }

    @Holding("this")
    @Override
    public synchronized ListenableFuture<RpcResult<ForceReloadOutput>> forceReload(ForceReloadInput input) {
        //take snapshot of configured peers
        final Set<Peer> configured = peerMap.values()
                .stream()
                .map(AbstractPeerContext::getPeer)
                .collect(Collectors.toSet());
        //cast is safe here, we populated peers from ConfiguredEndpoints objects
        configured.stream().map(ConfiguredEndpoints.class::cast).forEach(this::updatePeerContext);
        return Futures.immediateFuture(RpcResultBuilder.success(new ForceReloadOutputBuilder().build()).build());
    }
}
