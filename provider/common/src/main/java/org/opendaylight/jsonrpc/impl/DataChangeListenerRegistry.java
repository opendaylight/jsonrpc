/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.api.PeerContext;
import org.opendaylight.jsonrpc.bus.messagelib.PeerContextHolder;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.jsonrpc.model.DataChangeNotificationPublisher;
import org.opendaylight.jsonrpc.model.ListenerKey;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Registry of {@link DataChangeListenerRegistration}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Apr 17, 2018
 */
public class DataChangeListenerRegistry implements AutoCloseable {
    private final Map<ListenerKey, DataChangeListenerRegistration> listenerMap = new ConcurrentHashMap<>();
    private final DOMDataBroker domDataBroker;
    private final TransportFactory transportFactory;
    private final JsonRpcCodecFactory codecFactory;

    public DataChangeListenerRegistry(@NonNull final DOMDataBroker domDataBroker,
            @NonNull final TransportFactory transportFactory, @NonNull final JsonRpcCodecFactory codecFactory) {
        this.domDataBroker = Objects.requireNonNull(domDataBroker);
        this.transportFactory = Objects.requireNonNull(transportFactory);
        this.codecFactory = Objects.requireNonNull(codecFactory);
    }

    // suppress complains about using try-with-resources on publisher - we
    // purposely leave it open and close it later when no longer needed
    @SuppressWarnings("squid:S2095")
    public ListenerKey createListener(YangInstanceIdentifier path, LogicalDatastoreType store, String transport)
            throws IOException {
        final ListenerKey response = new ListenerKey(allocateUri(transport), UUID.randomUUID().toString());
        try {
            final DataChangeNotificationPublisher publisher = transportFactory.endpointBuilder()
                    .publisher()
                    .createProxy(DataChangeNotificationPublisher.class, response.getUri());
            listenerMap.put(response, new DataChangeListenerRegistration(path, listenerMap::remove, domDataBroker,
                    codecFactory, store, publisher, response));
            return response;
        } catch (URISyntaxException e) {
            // impossible to land here
            throw new IllegalStateException(e);
        }
    }

    public boolean removeListener(String uri, String name) {
        final DataChangeListenerRegistration listener = listenerMap.get(new ListenerKey(uri, name));
        if (listener != null) {
            listener.close();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void close() {
        listenerMap.values().stream().forEach(DataChangeListenerRegistration::close);
        listenerMap.clear();
    }

    /*
     * Allocate URI for notification publisher.
     */
    private String allocateUri(String transport) throws IOException {
        try (Socket socket = new Socket()) {
            socket.bind(null);
            final PeerContext peer = PeerContextHolder.get();
            return String.format("%s://%s:%d", transport != null ? transport : peer.transport(),
                    ((InetSocketAddress) peer.channel().localAddress()).getAddress().getHostAddress(),
                    socket.getLocalPort());
        }
    }
}
