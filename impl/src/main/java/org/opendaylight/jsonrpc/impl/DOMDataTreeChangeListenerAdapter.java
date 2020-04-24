/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.messagelib.SubscriberSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.model.DataChangeNotification;
import org.opendaylight.jsonrpc.model.DataChangeNotificationPublisher;
import org.opendaylight.jsonrpc.model.DataTreeCandidateImpl;
import org.opendaylight.jsonrpc.model.JSONRPCArg;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Adapter for {@link DOMDataTreeChangeListener} which subscribes to remote
 * event producer and call
 * {@link DOMDataTreeChangeListener#onDataTreeChanged(java.util.Collection)} on
 * event reception.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since May 6, 2018
 */
public class DOMDataTreeChangeListenerAdapter implements DataChangeNotificationPublisher, AutoCloseable {
    private final DOMDataTreeChangeListener listener;
    private final SubscriberSession session;
    private final JsonConverter converter;
    private final SchemaContext schemaContext;

    public DOMDataTreeChangeListenerAdapter(@NonNull DOMDataTreeChangeListener delegate,
            @NonNull TransportFactory transportFactory, String uri, @NonNull JsonConverter converter,
            @NonNull SchemaContext schemaContext) throws URISyntaxException {
        Objects.requireNonNull(transportFactory);
        this.converter = Objects.requireNonNull(converter);
        this.listener = Objects.requireNonNull(delegate);
        this.schemaContext = Objects.requireNonNull(schemaContext);
        this.session = transportFactory.endpointBuilder().subscriber().create(uri, this);
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
        final Set<DataTreeCandidate> changes = new LinkedHashSet<>();
        for (final JSONRPCArg c : change.getChanges()) {
            final YangInstanceIdentifier yii = YangInstanceIdentifierDeserializer.toYangInstanceIdentifier(c.getPath(),
                    schemaContext);
            final NormalizedNode<?, ?> data = converter.jsonElementToNormalizedNode(c.getData(), yii);
            changes.add(new DataTreeCandidateImpl(yii, data));
        }
        listener.onDataTreeChanged(changes);
    }
}
