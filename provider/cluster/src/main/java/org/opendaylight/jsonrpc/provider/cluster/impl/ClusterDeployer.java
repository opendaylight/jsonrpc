/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.impl;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.cluster.provider.config.rev200708.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.cluster.provider.config.rev200708.ConfigBuilder;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * A simple component publishing {@link Config} into the Service Registry.
 */
@Component(service = { })
public final class ClusterDeployer implements ClusteredDataTreeChangeListener<Config> {
    private static final @NonNull Config DEFAULT_CONFIG = new ConfigBuilder()
        .setActorResponseWaitTime(Uint16.TEN)
        .setRpcResponseWaitTime(Uint16.valueOf(30))
        .setWriteTransactionIdleTimeout(Uint16.valueOf(120))
        .build();
    
    private final BundleContext bundleContext;
    private final Registration dtclReg;
    
    private ServiceRegistration<?> configReg;
    
    @Activate
    public ClusterDeployer(@Reference DataBroker dataBroker, BundleContext bundleContext) {
        this.bundleContext = Objects.requireNonNull(bundleContext);
        this.dtclReg = dataBroker.registerDataTreeChangeListener(
            DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(Config.class)),
            this);
    }

    @Deactivate
    public void close() {
        dtclReg.close();
        unregisterConfig();
    }

    @Override
    public void onInitialData() {
        registerConfig(null);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Config>> changes) {
        unregisterConfig();
        registerConfig(Iterables.getLast(changes).getRootNode().getDataAfter());
    }
    
    private void registerConfig(@Nullable Config currentConfig) {
        final var effective = currentConfig != null ? currentConfig : DEFAULT_CONFIG;
        configReg = bundleContext.registerService(Config.class, effective, null);
    }

    private void unregisterConfig() {
        if (configReg != null) {
            configReg.unregister();
            configReg = null;
        }
    }
}
