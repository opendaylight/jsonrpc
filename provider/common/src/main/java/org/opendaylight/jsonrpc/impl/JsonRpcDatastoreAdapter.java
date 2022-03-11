/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static com.google.common.util.concurrent.Futures.getUnchecked;
import static org.opendaylight.jsonrpc.dom.codec.CodecUtils.decodeUnchecked;
import static org.opendaylight.jsonrpc.dom.codec.CodecUtils.encodeUnchecked;
import static org.opendaylight.jsonrpc.provider.common.Util.storeFromString;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.dom.codec.Codec;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.jsonrpc.model.AddListenerArgument;
import org.opendaylight.jsonrpc.model.DataOperationArgument;
import org.opendaylight.jsonrpc.model.DeleteListenerArgument;
import org.opendaylight.jsonrpc.model.ListenerKey;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.jsonrpc.model.StoreOperationArgument;
import org.opendaylight.jsonrpc.model.TxArgument;
import org.opendaylight.jsonrpc.model.TxOperationArgument;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for datastore operations.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 2, 2020
 */
public class JsonRpcDatastoreAdapter implements RemoteOmShard {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcDatastoreAdapter.class);
    private final JsonRpcCodecFactory codecFactory;
    private final TransactionManager txManager;
    private final DOMDataBroker domDataBroker;
    private final DataChangeListenerRegistry dataChangeRegistry;
    private Codec<JsonObject, YangInstanceIdentifier, RuntimeException> pathCodec;

    public JsonRpcDatastoreAdapter(@NonNull JsonRpcCodecFactory codecFactory, @NonNull DOMDataBroker domDataBroker,
            @NonNull EffectiveModelContext schemaContext, @NonNull TransportFactory transportFactory) {
        Objects.requireNonNull(schemaContext);
        Objects.requireNonNull(transportFactory);
        this.domDataBroker = Objects.requireNonNull(domDataBroker);
        this.codecFactory = Objects.requireNonNull(codecFactory);
        this.txManager = new TransactionManager(domDataBroker, schemaContext);
        this.dataChangeRegistry = new DataChangeListenerRegistry(domDataBroker, transportFactory, codecFactory);
        this.pathCodec = codecFactory.pathCodec();
    }

    @Override
    public JsonElement read(StoreOperationArgument arg) {
        final YangInstanceIdentifier path = pathCodec.deserialize(arg.getPath().getAsJsonObject());
        LOG.debug("READ : YII :{}", path);
        try (DOMDataTreeReadTransaction tx = domDataBroker.newReadOnlyTransaction()) {
            return encodeUnchecked(codecFactory, path,
                    getUnchecked(tx.read(storeFromString(arg.getStore()), path)).orElse(null));
        }
    }

    @Override
    public void put(DataOperationArgument arg) {
        final YangInstanceIdentifier path = pathCodec.deserialize(arg.getPath().getAsJsonObject());
        final LogicalDatastoreType store = storeFromString(arg.getStore());
        LOG.debug("PUT txId : {}, store : {}, entity : {}, path : {}, YII :{}, data : {}", arg.getTxid(), store,
                arg.getEntity(), arg.getPath(), path, arg.getData());
        final DOMDataTreeWriteTransaction wtx = txManager.allocate(arg.getTxid()).getValue().newWriteTransaction();
        wtx.put(store, path, decodeUnchecked(codecFactory, path, arg.getData()));
    }

    @Override
    public boolean exists(StoreOperationArgument arg) {
        final YangInstanceIdentifier path = pathCodec.deserialize(arg.getPath().getAsJsonObject());
        final LogicalDatastoreType store = storeFromString(arg.getStore());
        LOG.debug("EXISTS store={}, entity={}, path={}, YII={}", store, arg.getEntity(), arg.getPath(), path);
        try (DOMDataTreeReadTransaction tx = domDataBroker.newReadOnlyTransaction()) {
            return getUnchecked(tx.exists(store, path));
        }
    }

    @Override
    public void merge(DataOperationArgument arg) {
        final DOMDataTreeWriteTransaction trx = txManager.allocate(arg.getTxid()).getValue().newWriteTransaction();
        final YangInstanceIdentifier path = pathCodec.deserialize(arg.getPath().getAsJsonObject());
        final LogicalDatastoreType store = storeFromString(arg.getStore());
        LOG.debug("MERGE : tx={}, store={}, entity={}, path={}, YII={}, data={}", arg.getTxid(), store, arg.getEntity(),
                arg.getPath(), path, arg.getData());
        trx.merge(store, path, decodeUnchecked(codecFactory, path, arg.getData()));
    }

    @Override
    public void delete(TxOperationArgument arg) {
        final YangInstanceIdentifier path = pathCodec.deserialize(arg.getPath().getAsJsonObject());
        final LogicalDatastoreType store = storeFromString(arg.getStore());
        LOG.debug("DELETE : tx={}, store={}, entity={}, path={}, YII={}", arg.getTxid(), store, arg.getEntity(),
                arg.getPath(), path);
        final DOMDataTreeWriteTransaction trx = txManager.allocate(arg.getTxid()).getValue().newWriteTransaction();
        trx.delete(store, path);
    }

    @Override
    public boolean commit(TxArgument arg) {
        LOG.debug("COMMIT : {}", arg.getTxid());
        return txManager.commit(arg.getTxid());
    }

    @Override
    public boolean cancel(TxArgument arg) {
        LOG.debug("CANCEL : {}", arg.getTxid());
        return txManager.cancel(arg.getTxid());
    }

    @Override
    public String txid() {
        final String ret = txManager.allocate(null).getKey();
        LOG.debug("TXID : {}", ret);
        return ret;
    }

    @Override
    public List<String> error(TxArgument arg) {
        LOG.debug("ERROR : {}", arg.getTxid());
        return txManager.error(arg.getTxid());
    }

    @Override
    public ListenerKey addListener(AddListenerArgument arg) throws IOException {
        final YangInstanceIdentifier path = pathCodec.deserialize(arg.getPath().getAsJsonObject());
        return dataChangeRegistry.createListener(path, storeFromString(arg.getStore()), arg.getTransport());
    }

    @Override
    public boolean deleteListener(DeleteListenerArgument arg) {
        return dataChangeRegistry.removeListener(arg.getUri(), arg.getName());
    }

    @Override
    public void close() {
        txManager.close();
        dataChangeRegistry.close();
    }
}
