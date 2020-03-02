/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static org.opendaylight.jsonrpc.impl.Util.storeFromString;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.model.AddListenerArgument;
import org.opendaylight.jsonrpc.model.DataOperationArgument;
import org.opendaylight.jsonrpc.model.DeleteListenerArgument;
import org.opendaylight.jsonrpc.model.ListenerKey;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.jsonrpc.model.StoreOperationArgument;
import org.opendaylight.jsonrpc.model.TxArgument;
import org.opendaylight.jsonrpc.model.TxOperationArgument;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
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
    private final JsonConverter jsonConverter;
    private final TransactionManager txManager;
    private final DOMDataBroker domDataBroker;
    private final JsonRpcPathCodec pathCodec;
    private final DataChangeListenerRegistry dataChangeRegistry;
    // TODO : check if we can get rid of this
    private final boolean wrapBeforeMerge;

    public JsonRpcDatastoreAdapter(@NonNull JsonConverter jsonConverter, @NonNull DOMDataBroker domDataBroker,
            @NonNull SchemaContext schemaContext, @NonNull TransportFactory transportFactory, boolean wrapBeforeMerge) {
        Objects.requireNonNull(schemaContext);
        Objects.requireNonNull(transportFactory);
        this.domDataBroker = Objects.requireNonNull(domDataBroker);
        this.jsonConverter = Objects.requireNonNull(jsonConverter);
        this.txManager = new TransactionManager(domDataBroker, schemaContext);
        this.pathCodec = JsonRpcPathCodec.create(schemaContext);
        this.dataChangeRegistry = new DataChangeListenerRegistry(domDataBroker, transportFactory, jsonConverter);
        this.wrapBeforeMerge = wrapBeforeMerge;
    }

    @Override
    public JsonElement read(StoreOperationArgument arg) {
        final YangInstanceIdentifier path = pathCodec.deserialize(arg.getPath().getAsJsonObject());
        LOG.debug("READ : YII :{}", path);
        try (DOMDataTreeReadTransaction tx = domDataBroker.newReadOnlyTransaction()) {
            return jsonConverter.toBus(path, getUnchecked(tx.read(storeFromString(arg.getStore()), path)).orElse(null))
                    .getData();
        }
    }

    @Override
    public void put(DataOperationArgument arg) {
        final YangInstanceIdentifier path = pathCodec.deserialize(arg.getPath().getAsJsonObject());
        LOG.debug("PUT txId : {}, store : {}, entity : {}, path : {}, YII :{}, data : {}", arg.getTxid(),
                storeFromString(arg.getStore()), arg.getEntity(), arg.getPath(), path, arg.getData());
        final DOMDataTreeWriteTransaction wtx = txManager.allocate(arg.getTxid()).getValue().newWriteTransaction();
        wtx.put(storeFromString(arg.getStore()), path,
                jsonConverter.jsonElementToNormalizedNode(injectQName(path, arg.getData()), path));
    }

    @Override
    public boolean exists(StoreOperationArgument arg) {
        final YangInstanceIdentifier path = pathCodec.deserialize(arg.getPath().getAsJsonObject());
        LOG.debug("EXISTS store={}, entity={}, path={}, YII={}", storeFromString(arg.getStore()), arg.getEntity(),
                arg.getPath(), path);
        try (DOMDataTreeReadTransaction tx = domDataBroker.newReadOnlyTransaction()) {
            return getUnchecked(tx.exists(storeFromString(arg.getStore()), path));
        }
    }

    @Override
    public void merge(DataOperationArgument arg) {
        final DOMDataTreeWriteTransaction trx = txManager.allocate(arg.getTxid()).getValue().newWriteTransaction();
        final YangInstanceIdentifier path = pathCodec.deserialize(arg.getPath().getAsJsonObject());
        LOG.info("MERGE : tx={}, store={}, entity={}, path={}, YII={}, data={}", arg.getTxid(),
                storeFromString(arg.getStore()), arg.getEntity(), arg.getPath(), path, arg.getData());
        trx.merge(storeFromString(arg.getStore()), path,
                jsonConverter.jsonElementToNormalizedNode(arg.getData(), path, wrapBeforeMerge));
    }

    @Override
    public void delete(TxOperationArgument arg) {
        final YangInstanceIdentifier path = pathCodec.deserialize(arg.getPath().getAsJsonObject());
        LOG.debug("DELETE : tx={}, store={}, entity={}, path={}, YII={}", arg.getTxid(),
                storeFromString(arg.getStore()), arg.getEntity(), arg.getPath(), path);
        final DOMDataTreeWriteTransaction trx = txManager.allocate(arg.getTxid()).getValue().newWriteTransaction();
        trx.delete(storeFromString(arg.getStore()), path);
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

    private <V> V getUnchecked(Future<V> future) {
        try {
            return Uninterruptibles.getUninterruptibly(future);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Unable to get result of future", e);
        }
    }

    /**
     * Helper method to achieve compatibility between JSON-RPC data spec and
     * <a href="https://tools.ietf.org/html/rfc7951">RFC-7951</a><br />
     * <strong>Incompatibility example:</strong> YANG model (in module 'test-model'):
     *
     * <pre>
     * container grillconf {
     *     leaf gasKnob {
     *         type unit32;
     *     }
     * }
     * </pre>
     *
     * <p>
     * To set leaf 'gasKnob' to value eg. 5, path is
     *
     * <pre>
     * {"test-model:grillconf":{}}
     * </pre>
     *
     * <p>
     * and data is
     *
     * <pre>
     * {"gasKnob":10}
     * </pre>
     *
     * <p>
     * This breaks requirement of GSON codec, which needs data to be qualified by module name. Injecting/wrapping
     * qualifier around it can workaround this issue, so outcome of this method will be such as:
     *
     * <pre>
     *  {"test-model:grillconf":{"gasKnob":5}}
     * </pre>
     *
     * @param yii {@link YangInstanceIdentifier} of container node
     * @param inJson input JSON to be wrapped with qualifier
     * @return wrapped JSON with injected qualifier
     */
    private JsonElement injectQName(YangInstanceIdentifier yii, JsonElement inJson) {
        LOG.debug("Injecting QName from {} into JSON '{}'", yii, inJson);
        final Set<Entry<String, JsonElement>> fields = ((JsonObject) inJson).entrySet();
        // nothing to wrap
        if (fields.isEmpty()) {
            return inJson;
        }
        final Entry<String, JsonElement> firstNode = fields.iterator().next();
        // already qualified
        if (firstNode.getKey().indexOf(':') != -1) {
            return inJson;
        }
        final QName qn = yii.getLastPathArgument().getNodeType();
        final JsonObject wrapper = new JsonObject();
        wrapper.add(qn.getLocalName(), inJson);
        LOG.debug("Wrapped data : {}", wrapper);
        return wrapper;
    }
}
