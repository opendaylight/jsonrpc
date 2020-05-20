/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.provider.cluster.api.JsonRpcPeerSingletonService;
import org.opendaylight.jsonrpc.provider.common.ProviderDependencies;
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
 * @XXX FROM: NetconfTopologyManager
 */
@Singleton
@Component(immediate = true)
public class JsonRpcPeerListManager
        implements ClusteredDataTreeChangeListener<ConfiguredEndpoints>, JsonRpcPeerSingletonService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcPeerListManager.class);
    private final ListenerRegistration<JsonRpcPeerListManager> dtcListener;
    private final Map<String, Object> peerMap = new HashMap<>();

    public JsonRpcPeerListManager(ProviderDependencies dependencies) {
        this.dtcListener = dependencies.getDataBroker()
                .registerDataTreeChangeListener(ClusterUtil.getPeerListIdentifier(), this);
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
                    // TODO : Peer updates are not (yet) supported
                    break;
                case WRITE:
                    if (peerMap.containsKey(name)) {
                        // TODO : Peer updates are not (yet) supported
                    } else {
                        LOG.debug("Peer entry '{}' created", name);
                        createPeerContext(rootNode.getDataAfter());
                    }
                    break;
                case DELETE:
                    LOG.debug("Peer entry '{}' removed", name);
                    destroyPeerContext(name);
                    break;
                default:
                    LOG.warn("Unhandled data modification", rootNode.getModificationType());
            }
        }
    }

    private void destroyPeerContext(String name) {
        // TODO Auto-generated method stub

    }

    private void createPeerContext(ConfiguredEndpoints dataAfter) {
        // TODO Auto-generated method stub

    }

    @Deactivate
    @Override
    public void close() {
        dtcListener.close();
    }
}
