/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.jsonrpc.model.DataChangeNotification;
import org.opendaylight.jsonrpc.model.DataChangeNotificationPublisher;
import org.opendaylight.jsonrpc.model.JSONRPCArg;
import org.opendaylight.jsonrpc.model.ListenerKey;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBroker.DataTreeChangeExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is {@link DOMDataTreeChangeListener} and its registration object at same time. It forwards data change
 * event to remote subscriber and perform necessary cleanup when no longer needed.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since May 8, 2018
 */
public final class DataChangeListenerRegistration implements Registration, DOMDataTreeChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(DataChangeListenerRegistration.class);
    private final DataChangeNotificationPublisher publisher;
    private final YangInstanceIdentifier path;
    private final Registration delegate;
    private final JsonRpcCodecFactory codecFactory;
    private final Consumer<ListenerKey> closeCallback;
    private final ListenerKey listenerKey;

    public DataChangeListenerRegistration(@NonNull YangInstanceIdentifier path,
            @NonNull Consumer<ListenerKey> closeCallback, @NonNull final DOMDataBroker domDataBroker,
            @NonNull final JsonRpcCodecFactory codecFactory, @NonNull LogicalDatastoreType store,
            @NonNull DataChangeNotificationPublisher publisher, @NonNull ListenerKey listenerKey) {
        this.path = Objects.requireNonNull(path);
        this.closeCallback = Objects.requireNonNull(closeCallback);
        this.codecFactory = Objects.requireNonNull(codecFactory);
        this.publisher = Objects.requireNonNull(publisher);
        this.listenerKey = Objects.requireNonNull(listenerKey);
        Objects.requireNonNull(domDataBroker);
        Objects.requireNonNull(store);
        final var dtcs = domDataBroker.extension(DataTreeChangeExtension.class);
        delegate = dtcs.registerTreeChangeListener(DOMDataTreeIdentifier.of(store, path), this);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public void close() {
        LOG.debug("Closing notification publisher for path {}", path);
        try {
            Stream.of(publisher, delegate).forEach(Util::closeAndLogOnError);
            closeCallback.accept(listenerKey);
        } catch (Exception e) {
            throw new IllegalStateException("Failed while closing registration", e);
        }
    }

    @Override
    public void onInitialData() {
        // TODO: do something?
    }

    @Override
    public void onDataTreeChanged(List<DataTreeCandidate> treeChanges) {
        final DataChangeNotification dcn = new DataChangeNotification(
                treeChanges.stream().map(this::mapDtc).collect(Collectors.toSet()));
        LOG.debug("Sending notification {}", dcn);
        publisher.notifyListener(dcn);
    }

    private JSONRPCArg mapDtc(DataTreeCandidate dtc) {
        final JsonObject jsonpath = codecFactory.pathCodec().serialize(dtc.getRootPath());
        try {
            final JsonElement data = codecFactory.dataCodec(dtc.getRootPath()).serialize(dtc.getRootNode().dataAfter());
            return new JSONRPCArg(jsonpath, data);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
