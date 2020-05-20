/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMNotificationPublishService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.yangtools.yang.xpath.api.YangXPathParserFactory;

public class JsonRpcProviderDependencies {
    private final TransportFactory transportFactory;
    private final DataBroker dataBroker;
    private final DOMMountPointService domMountPointService;
    private final DOMDataBroker domDataBroker;
    private final DOMSchemaService schemaService;
    private final DOMNotificationPublishService domNotificationPublishService;
    private final DOMRpcService domRpcService;
    private final ClusterSingletonServiceProvider clusterSingletonServiceProvider;
    private YangXPathParserFactory yangXPathParserFactory;

    public JsonRpcProviderDependencies(TransportFactory transportFactory, DataBroker dataBroker,
            DOMMountPointService domMountPointService, DOMDataBroker domDataBroker, DOMSchemaService schemaService,
            DOMNotificationPublishService domNotificationPublishService, DOMRpcService domRpcService,
            ClusterSingletonServiceProvider clusterSingletonServiceProvider,
            YangXPathParserFactory yangXPathParserFactory) {
        this.transportFactory = transportFactory;
        this.dataBroker = dataBroker;
        this.domMountPointService = domMountPointService;
        this.domDataBroker = domDataBroker;
        this.schemaService = schemaService;
        this.domNotificationPublishService = domNotificationPublishService;
        this.domRpcService = domRpcService;
        this.clusterSingletonServiceProvider = clusterSingletonServiceProvider;
        this.yangXPathParserFactory = yangXPathParserFactory;
    }

    public TransportFactory getTransportFactory() {
        return transportFactory;
    }

    public DataBroker getDataBroker() {
        return dataBroker;
    }

    public DOMMountPointService getDomMountPointService() {
        return domMountPointService;
    }

    public DOMDataBroker getDomDataBroker() {
        return domDataBroker;
    }

    public DOMSchemaService getSchemaService() {
        return schemaService;
    }

    public DOMNotificationPublishService getDomNotificationPublishService() {
        return domNotificationPublishService;
    }

    public DOMRpcService getDomRpcService() {
        return domRpcService;
    }

    public ClusterSingletonServiceProvider getClusterSingletonServiceProvider() {
        return clusterSingletonServiceProvider;
    }

    public YangXPathParserFactory getYangXPathParserFactory() {
        return yangXPathParserFactory;
    }
}
