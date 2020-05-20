/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.impl;

import akka.actor.ActorSystem;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.provider.common.ProviderDependencies;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMNotificationPublishService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.yangtools.yang.xpath.api.YangXPathParserFactory;

public class ClusterDependencies extends ProviderDependencies {
    private final ActorSystem actorSystem;
    private final ClusterSingletonServiceProvider clusterSingletonServiceProvider;

    public ClusterDependencies(@NonNull TransportFactory transportFactory, @NonNull DataBroker dataBroker,
            @NonNull DOMMountPointService domMountPointService, @NonNull DOMDataBroker domDataBroker,
            @NonNull DOMSchemaService schemaService,
            @NonNull DOMNotificationPublishService domNotificationPublishService, @NonNull DOMRpcService domRpcService,
            @NonNull YangXPathParserFactory yangXPathParserFactory, @NonNull ActorSystem actorSystem,
            @NonNull ClusterSingletonServiceProvider clusterSingletonServiceProvider) {
        super(transportFactory, dataBroker, domMountPointService, domDataBroker, schemaService,
                domNotificationPublishService, domRpcService, yangXPathParserFactory);
        this.actorSystem = Objects.requireNonNull(actorSystem);
        this.clusterSingletonServiceProvider = Objects.requireNonNull(clusterSingletonServiceProvider);
    }

    public ActorSystem getActorSystem() {
        return actorSystem;
    }

    public ClusterSingletonServiceProvider getClusterSingletonServiceProvider() {
        return clusterSingletonServiceProvider;
    }
}
