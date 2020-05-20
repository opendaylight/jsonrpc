/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.impl;

import akka.util.Timeout;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.provider.cluster.api.JsonRpcPeerSingletonService;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpoints;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that monitors list of {@link Peer}s in config DS and create/destroy mountpoints accordingly. Lifecycle of
 * this service is managed by blueprint.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 1, 2020
 */
@Singleton
@Component(immediate = true)
public class JsonRpcPeerListManager
        implements ClusteredDataTreeChangeListener<ConfiguredEndpoints>, JsonRpcPeerSingletonService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcPeerListManager.class);
    private final ListenerRegistration<JsonRpcPeerListManager> dtcListener;
    private final Map<String, JsonRpcPeerManager> peerMap = new HashMap<>();
    private final ClusterDependencies dependencies;
    private final Timeout askTimeout;

    public JsonRpcPeerListManager(ClusterDependencies dependencies, Timeout askTimeout) {
        this.dependencies = dependencies;
        this.dtcListener = dependencies.getDataBroker()
                .registerDataTreeChangeListener(ClusterUtil.getPeerListIdentifier(), this);
        this.askTimeout = askTimeout;
    }

    @Override
    public void onDataTreeChanged(@NonNull Collection<DataTreeModification<ConfiguredEndpoints>> changes) {
        for (final DataTreeModification<ConfiguredEndpoints> change : changes) {
            final DataObjectModification<ConfiguredEndpoints> rootNode = change.getRootNode();
            final InstanceIdentifier<ConfiguredEndpoints> ident = change.getRootPath().getRootIdentifier();
            final String name = ClusterUtil.peerNameFromII(ident);
            LOG.debug("DTC [{}] : {} => {}", rootNode.getModificationType(), rootNode.getDataBefore(),
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
                    LOG.warn("Unhandled data modification", rootNode.getModificationType());
            }
        }
    }

    private void updatePeerContext(ConfiguredEndpoints peer) {
        if (peerMap.containsKey(peer.getName())) {
            peerMap.get(peer.getName()).reset(peer);
        }
    }

    private void destroyPeerContext(String name) {
        LOG.info("Removing context '{}'", name);
        Optional.ofNullable(peerMap.remove(name)).ifPresent(JsonRpcPeerManager::close);
    }

    private void createPeerContext(ConfiguredEndpoints peer) {
        LOG.info("Creating context for '{}'", peer.getName());
        final JsonRpcPeerManager service = new JsonRpcPeerManager(dependencies, peer, askTimeout);
        LOG.info("Created {}", service);
        peerMap.put(peer.getName(), service);
    }

    @Deactivate
    @Override
    public void close() {
        dtcListener.close();
        peerMap.values().forEach(JsonRpcPeerManager::close);
        peerMap.clear();
    }
}
