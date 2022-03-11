/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.messagelib.SubscriberSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.jsonrpc.model.DataChangeNotification;
import org.opendaylight.jsonrpc.model.DataChangeNotificationPublisher;
import org.opendaylight.jsonrpc.model.JSONRPCArg;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidates;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for {@link DOMDataTreeChangeListener} which subscribes to remote
 * event producer and call {@link DOMDataTreeChangeListener#onDataTreeChanged(java.util.List)} on
 * event reception.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since May 6, 2018
 */
public final class DOMDataTreeChangeListenerAdapter implements DataChangeNotificationPublisher, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DOMDataTreeChangeListenerAdapter.class);
    private final DOMDataTreeChangeListener listener;
    private final SubscriberSession session;
    private final JsonRpcCodecFactory codecFactory;

    public DOMDataTreeChangeListenerAdapter(@NonNull DOMDataTreeChangeListener delegate,
            @NonNull TransportFactory transportFactory, String uri, @NonNull JsonRpcCodecFactory codecFactory,
            @NonNull SchemaContext schemaContext) throws URISyntaxException {
        Objects.requireNonNull(transportFactory);
        this.listener = Objects.requireNonNull(delegate);
        this.session = transportFactory.endpointBuilder().subscriber().create(uri, this);
        this.codecFactory = Objects.requireNonNull(codecFactory);
    }

    /**
     * Called when registration object of listener is about to close.
     */
    @Override
    public void close() {
        session.close();
    }

    @Override
    public void notifyListener(DataChangeNotification change) {
        final List<DataTreeCandidate> changes = new ArrayList<>();
        for (final JSONRPCArg c : change.getChanges()) {
            final YangInstanceIdentifier yii = codecFactory.pathCodec().deserialize(c.getPath().getAsJsonObject());
            try {
                final NormalizedNode data = codecFactory.dataCodec(yii).deserialize(c.getData());
                changes.add(DataTreeCandidates.fromNormalizedNode(yii, data));
            } catch (IOException e) {
                LOG.error("Unable to deserialize DCN {}", c.getData(), e);
            }
        }
        listener.onDataTreeChanged(changes);
    }
}
