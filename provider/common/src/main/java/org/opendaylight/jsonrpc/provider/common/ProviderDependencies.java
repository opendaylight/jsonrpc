/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMNotificationPublishService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.yang.model.spi.source.YangTextToIRSourceTransformer;
import org.opendaylight.yangtools.yang.parser.api.YangParserFactory;

/**
 * Simple grouping of all required dependencies for easier propagation.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 2, 2020
 */
public class ProviderDependencies {
    private final TransportFactory transportFactory;
    private final DataBroker dataBroker;
    private final DOMMountPointService domMountPointService;
    private final DOMDataBroker domDataBroker;
    private final DOMSchemaService schemaService;
    private final DOMNotificationPublishService domNotificationPublishService;
    private final DOMRpcService domRpcService;
    private final YangParserFactory yangParserFactory;
    private final YangTextToIRSourceTransformer yangTextToIR;

    public ProviderDependencies(@NonNull TransportFactory transportFactory, @NonNull DataBroker dataBroker,
            @NonNull DOMMountPointService domMountPointService, @NonNull DOMDataBroker domDataBroker,
            @NonNull DOMSchemaService schemaService,
            @NonNull DOMNotificationPublishService domNotificationPublishService, @NonNull DOMRpcService domRpcService,
            @NonNull YangParserFactory yangParserFactory, @NonNull YangTextToIRSourceTransformer yangTextToIR) {
        this.transportFactory = Objects.requireNonNull(transportFactory);
        this.dataBroker = Objects.requireNonNull(dataBroker);
        this.domMountPointService = Objects.requireNonNull(domMountPointService);
        this.domDataBroker = Objects.requireNonNull(domDataBroker);
        this.schemaService = Objects.requireNonNull(schemaService);
        this.domNotificationPublishService = Objects.requireNonNull(domNotificationPublishService);
        this.domRpcService = Objects.requireNonNull(domRpcService);
        this.yangParserFactory = Objects.requireNonNull(yangParserFactory);
        this.yangTextToIR = Objects.requireNonNull(yangTextToIR);
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

    public YangParserFactory getYangParserFactory() {
        return yangParserFactory;
    }

    public YangTextToIRSourceTransformer getYangTextToIR() {
        return yangTextToIR;
    }
}
