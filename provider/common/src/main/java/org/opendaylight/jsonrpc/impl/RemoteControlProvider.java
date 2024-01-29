/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.messagelib.ResponderSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.jsonrpc.model.GovernanceProvider;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.provider.common.ProviderDependencies;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMNotificationPublishService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.xpath.api.YangXPathParserFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages lifecycle of governance proxy and remote control (who-am-i) server.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 2, 2020
 */
@Component(service = GovernanceProvider.class)
public final class RemoteControlProvider
        implements AutoCloseable, DataTreeChangeListener<Config>, GovernanceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteControlProvider.class);
    private final Registration registration;
    private final ProviderDependencies dependencies;
    private final JsonRpcCodecFactory codecFactory;
    private String remoteControlUri;
    private String governanceRootUri;
    private RemoteGovernance governance;
    private ResponderSession remoteControl;

    @Activate
    public RemoteControlProvider(@Reference YangXPathParserFactory yangXPathParserFactory,
            @Reference DataBroker dataBroker, @Reference DOMDataBroker domDataBroker,
            @Reference DOMMountPointService domMountPointService, @Reference DOMSchemaService schemaService,
            @Reference DOMRpcService domRpcService,
            @Reference DOMNotificationPublishService domNotificationPublishService,
            @Reference TransportFactory transportFactory) {
        this(new ProviderDependencies(transportFactory, dataBroker, domMountPointService, domDataBroker, schemaService,
            domNotificationPublishService, domRpcService, yangXPathParserFactory));
    }

    public RemoteControlProvider(@NonNull ProviderDependencies dependencies) {
        this.dependencies = Objects.requireNonNull(dependencies);
        this.codecFactory = new JsonRpcCodecFactory(dependencies.getSchemaService().getGlobalContext());
        registration = dependencies.getDataBroker()
                .registerTreeChangeListener(DataTreeIdentifier.of(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.create(Config.class)), this);
    }

    @Override
    public void onDataTreeChanged(List<DataTreeModification<Config>> changes) {
        for (DataTreeModification<Config> change : changes) {
            final DataObjectModification<Config> root = change.getRootNode();
            final ModificationType type = root.modificationType();
            switch (type) {
                case DELETE:
                    final var dataAfter = root.dataAfter();
                    if (dataAfter == null || dataAfter.getGovernanceRoot() == null) {
                        stopGovernance();
                    }
                    if (dataAfter == null || dataAfter.getWhoAmI() == null) {
                        stopRemoteControl();
                    }
                    break;

                case WRITE:
                case SUBTREE_MODIFIED:
                    reconfigureServices(root.dataAfter());
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
