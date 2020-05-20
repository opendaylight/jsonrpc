/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.remove;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.provider.cluster.impl.ClusterDependencies;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Component(immediate = true)
public class ClusteredProvider implements ClusterSingletonService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ClusteredProvider.class);
    private static final ServiceGroupIdentifier SGI = ServiceGroupIdentifier.create("jsonrpc/provider");
    private ClusterDependencies dependencies;
    private ClusterSingletonServiceRegistration registration;
    private SlaveProvider slaveProvider;
    private MasterProvider masterProvider;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ClusteredProvider(ClusterDependencies dependencies) {
        this.dependencies = Objects.requireNonNull(dependencies);
    }

    @Activate
    public void init() {
        registration = dependencies.getClusterSingletonServiceProvider().registerClusterSingletonService(this);
        slaveProvider = new SlaveProvider();
        masterProvider = new MasterProvider(null, dependencies);
    }

    @Deactivate
    @Override
    public void close() throws Exception {
        LOG.info("Closing {}", this);
        if (!closed.compareAndSet(false, true)) {
            registration.close();
            slaveProvider.close();
            masterProvider.close();
        }
    }

    @Override
    public @NonNull ServiceGroupIdentifier getIdentifier() {
        return SGI;
    }

    @Override
    public void instantiateServiceInstance() {
        masterProvider.enable();
        slaveProvider.disable();
    }

    @Override
    public ListenableFuture<? extends Object> closeServiceInstance() {
        masterProvider.disable();
        slaveProvider.enable();
        return FluentFutures.immediateNullFluentFuture();
    }
}
