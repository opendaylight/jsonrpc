/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static org.opendaylight.jsonrpc.provider.common.Util.findNode;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.model.AddListenerArgument;
import org.opendaylight.jsonrpc.model.DataOperationArgument;
import org.opendaylight.jsonrpc.model.DeleteListenerArgument;
import org.opendaylight.jsonrpc.model.ListenerKey;
import org.opendaylight.jsonrpc.model.RemoteControlComposite;
import org.opendaylight.jsonrpc.model.StoreOperationArgument;
import org.opendaylight.jsonrpc.model.TxArgument;
import org.opendaylight.jsonrpc.model.TxOperationArgument;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMNotification;
import org.opendaylight.mdsal.dom.api.DOMNotificationPublishService;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RemoteControl implements RemoteControlComposite {
    private final SchemaContext schemaContext;
    private final JsonConverter jsonConverter;
    private final ConcurrentMap<String, DataModificationContext> txmap = new ConcurrentHashMap<>();
    private final DOMNotificationPublishService publishService;
    private final DOMRpcService rpcService;
    private final JsonRpcDatastoreAdapter datastore;

    public RemoteControl(@NonNull final DOMDataBroker domDataBroker, @NonNull final SchemaContext schemaContext,
            @NonNull TransportFactory transportFactory, @NonNull final DOMNotificationPublishService publishService,
            @NonNull final DOMRpcService rpcService) {
        this.schemaContext = Objects.requireNonNull(schemaContext);
        this.jsonConverter = new JsonConverter(schemaContext);
        this.publishService = Objects.requireNonNull(publishService);
        this.rpcService = Objects.requireNonNull(rpcService);
        this.datastore = new JsonRpcDatastoreAdapter(jsonConverter, domDataBroker, schemaContext, transportFactory,
                true);
    }

    @VisibleForTesting
    boolean isTxMapEmpty() {
        return txmap.entrySet().isEmpty();
    }

    @Override
    public JsonElement read(StoreOperationArgument arg) {
        return datastore.read(arg);
    }

    @Override
    public void put(DataOperationArgument arg) {
        datastore.put(arg);
    }

    public boolean exists(StoreOperationArgument arg) {
        return datastore.exists(arg);
    }

    @Override
    public void merge(DataOperationArgument arg) {
        datastore.merge(arg);
    }

    public void delete(TxOperationArgument arg) {
        datastore.delete(arg);
    }

    @Override
    public String txid() {
        return datastore.txid();
    }

    @Override
    public boolean commit(TxArgument arg) {
        return datastore.commit(arg);
    }

    @Override
    public boolean cancel(TxArgument arg) {
        return datastore.cancel(arg);
    }

    @Override
    public List<String> error(TxArgument arg) {
        return datastore.error(arg);
    }

    @Override
    public ListenerKey addListener(AddListenerArgument arg) throws IOException {
        return datastore.addListener(arg);
    }

    @Override
    public boolean deleteListener(DeleteListenerArgument arg) {
        return datastore.deleteListener(arg);
    }

    @Override
    public void close() {
        datastore.close();
    }

    @Override
    public JsonElement invokeRpc(String name, JsonObject rpcInput) {
        final RpcDefinition def = findNode(schemaContext, name, Module::getRpcs)
                .orElseThrow(() -> new IllegalArgumentException("No such method " + name));
        final JsonObject wrapper = new JsonObject();
        wrapper.add("input", rpcInput);
        final NormalizedNode<?, ?> nn = jsonConverter.rpcInputConvert(def, wrapper);
        try {
            final DOMRpcResult out = Uninterruptibles.getUninterruptibly(rpcService.invokeRpc(def.getPath(), nn));
            if (!out.getErrors().isEmpty()) {
                throw new IllegalStateException("RPC invocation failed : " + out.getErrors());
            }
            return out.getResult() == null ? JsonNull.INSTANCE
                    : jsonConverter.rpcConvert(def.getOutput().getPath(), (ContainerNode) out.getResult());
        } catch (ExecutionException e) {
            throw new IllegalStateException("RPC invocation failed", e);
        }
    }

    @Override
    public void publishNotification(String name, JsonObject data) {
        final NotificationDefinition notification = findNode(schemaContext, name, Module::getNotifications)
                .orElseThrow(() -> new IllegalArgumentException("No such notification : " + name));
        final DOMNotification dom = jsonConverter.toNotification(notification, data);
        try {
            Uninterruptibles.getUninterruptibly(publishService.offerNotification(dom));
        } catch (ExecutionException e) {
            throw new IllegalStateException("Notification delivery failed", e);
        }
    }
}
