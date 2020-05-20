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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.provider.cluster.api.JsonRpcPeerSingletonService;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpoints;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that monitors list of {@link Peer}s in config DS and create/destroy mountpoints accordingly. Lifecycle of
 * this service is managed by blueprint.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 1, 2020
 */
public class JsonRpcPeerListManager
        implements ClusteredDataTreeChangeListener<ConfiguredEndpoints>, JsonRpcPeerSingletonService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcPeerListManager.class);
    private final ListenerRegistration<JsonRpcPeerListManager> dtcListener;
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

    @SuppressFBWarnings(value = "UCF_USELESS_CONTROL_FLOW",
            justification = "Kept here as a reminder to implement updates later")
    private void updatePeerContext(ConfiguredEndpoints peer) {
        if (peerMap.containsKey(peer.getName())) {
            // TODO : implement updates later?
        }
    }

    private void destroyPeerContext(String name) {
        LOG.info("Removing context '{}'", name);
        removeFromMapAndClose(peerMap, name);
        removeFromMapAndClose(clusterRegistrations, name);
    }

    private void createPeerContext(ConfiguredEndpoints peer) {
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
}
