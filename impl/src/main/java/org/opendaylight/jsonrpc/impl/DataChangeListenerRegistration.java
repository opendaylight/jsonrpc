/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.model.DataChangeNotification;
import org.opendaylight.jsonrpc.model.DataChangeNotificationPublisher;
import org.opendaylight.jsonrpc.model.ListenerKey;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is {@link DOMDataTreeChangeListener} and its registration object
 * at same time. It forwards data change event to remote subscriber and perform
 * necessary cleanup when no longer needed.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since May 8, 2018
 */
public class DataChangeListenerRegistration
        implements ListenerRegistration<DOMDataTreeChangeListener>, DOMDataTreeChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(DataChangeListenerRegistration.class);
    private final DataChangeNotificationPublisher publisher;
    private final YangInstanceIdentifier path;
    private final ListenerRegistration<DataChangeListenerRegistration> delegate;
    private final JsonConverter converter;
    private final Consumer<ListenerKey> closeCallback;
    private ListenerKey listenerKey;

    public DataChangeListenerRegistration(@NonNull YangInstanceIdentifier path,
            @NonNull Consumer<ListenerKey> closeCallback, @NonNull final DOMDataBroker domDataBroker,
            @NonNull final JsonConverter converter, @NonNull LogicalDatastoreType store,
            @NonNull DataChangeNotificationPublisher publisher, @NonNull ListenerKey listenerKey) {
        this.path = Objects.requireNonNull(path);
        this.closeCallback = Objects.requireNonNull(closeCallback);
        this.converter = Objects.requireNonNull(converter);
        this.publisher = Objects.requireNonNull(publisher);
        this.listenerKey = Objects.requireNonNull(listenerKey);
        Objects.requireNonNull(domDataBroker);
        Objects.requireNonNull(store);
        final DOMDataTreeChangeService dtcs = (DOMDataTreeChangeService) domDataBroker.getExtensions()
                .get(DOMDataTreeChangeService.class);
        delegate = dtcs.registerDataTreeChangeListener(new DOMDataTreeIdentifier(store, path), this);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public void close() {
        LOG.debug("Closing notification publisher for path {}", path);
        try {
            Stream.of(publisher, delegate).forEach(
                c -> Util.closeNullableWithExceptionCallback(c, e -> LOG.warn("Unable to close {}", c, e)));
            closeCallback.accept(listenerKey);
        } catch (Exception e) {
            throw new IllegalStateException("Failed while closing registration", e);
        }
    }

    @Override
    public DOMDataTreeChangeListener getInstance() {
        return this;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeCandidate> treeChanges) {
        final DataChangeNotification dcn = new DataChangeNotification(treeChanges.stream()
                .map(dtc -> converter.toBus(dtc.getRootPath(), dtc.getRootNode().getDataAfter().orElse(null)))
                .collect(Collectors.toSet()));
        LOG.debug("Sending notification {}", dcn);
        publisher.notifyListener(dcn);
    }
}
