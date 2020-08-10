/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.messagelib.ResponderSession;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.jsonrpc.model.GovernanceProvider;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.provider.common.ProviderDependencies;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages lifecycle of governance proxy and remote control (who-am-i) server.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 2, 2020
 */
public class RemoteControlProvider
        implements AutoCloseable, ClusteredDataTreeChangeListener<Config>, GovernanceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteControlProvider.class);
    private final ListenerRegistration<RemoteControlProvider> registration;
    private final ProviderDependencies dependencies;
    private final JsonRpcCodecFactory codecFactory;
    private String remoteControlUri;
    private String governanceRootUri;
    private RemoteGovernance governance;
    private ResponderSession remoteControl;

    public RemoteControlProvider(@NonNull ProviderDependencies dependencies) {
        registration = dependencies.getDataBroker()
                .registerDataTreeChangeListener(DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.builder(Config.class).build()), this);
        this.dependencies = Objects.requireNonNull(dependencies);
        this.codecFactory = new JsonRpcCodecFactory(dependencies.getSchemaService().getGlobalContext());
    }

    @Override
    public void onDataTreeChanged(@NonNull Collection<DataTreeModification<Config>> changes) {
        for (DataTreeModification<Config> change : changes) {
            final DataObjectModification<Config> root = change.getRootNode();
            final ModificationType type = root.getModificationType();
            switch (type) {
                case DELETE:
                    if (root.getDataAfter() == null || root.getDataAfter().getGovernanceRoot() == null) {
                        stopGovernance();
                    }
                    if (root.getDataAfter() == null || root.getDataAfter().getWhoAmI() == null) {
                        stopRemoteControl();
                    }
                    break;

                case WRITE:
                case SUBTREE_MODIFIED:
                    reconfigureServices(root.getDataAfter());
                    break;

                default:
                    break;
            }
        }
    }

    @Override
    public void close() throws Exception {
        stopGovernance();
        stopRemoteControl();
        registration.close();
    }

    @Override
    public Optional<RemoteGovernance> get() {
        return Optional.ofNullable(governance);
    }

    private void reconfigureServices(Config config) {
        try {
            resetGovernance(config.getGovernanceRoot());
            resetRemoteControl(config.getWhoAmI());
        } catch (URISyntaxException e) {
            LOG.error("Invalid URI was specified", e);
        }
    }

    private boolean resetRemoteControl(Uri whoAmI) throws URISyntaxException {
        if (whoAmI != null) {
            if (!whoAmI.getValue().equals(remoteControlUri)) {
                stopRemoteControl();
                remoteControlUri = whoAmI.getValue();
                LOG.debug("Exposing remote control at {}", whoAmI);
                remoteControl = dependencies.getTransportFactory()
                        .endpointBuilder()
                        .responder()
                        .create(whoAmI.getValue(), new RemoteControl(dependencies.getDomDataBroker(),
                                dependencies.getSchemaService().getGlobalContext(), dependencies.getTransportFactory(),
                                dependencies.getDomNotificationPublishService(), dependencies.getDomRpcService(),
                                codecFactory));
            }
        } else {
            remoteControl = null;
            LOG.debug("Remote control not configured");
        }
        return true;
    }

    private boolean resetGovernance(final Uri root) throws URISyntaxException {
        if (root != null) {
            if (!root.getValue().equals(governanceRootUri)) {
                stopGovernance();
                governanceRootUri = root.getValue();
                LOG.debug("(Re)setting governance root to {}", root.getValue());
                governance = dependencies.getTransportFactory()
                        .endpointBuilder()
                        .requester()
                        .createProxy(RemoteGovernance.class, root.getValue());
            }
        } else {
            governance = null;
            LOG.debug("Governance not configured");
        }
        return true;
    }

    private void stopRemoteControl() {
        Util.closeAndLogOnError(remoteControl);
        remoteControl = null;
        remoteControlUri = null;
    }

    private void stopGovernance() {
        Util.closeAndLogOnError(governance);
        governance = null;
        governanceRootUri = null;
    }
}
