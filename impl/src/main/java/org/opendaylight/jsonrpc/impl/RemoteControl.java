/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static org.opendaylight.jsonrpc.impl.Util.int2store;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.model.ListenerKey;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.jsonrpc.model.TransactionFactory;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteControl implements RemoteOmShard, AutoCloseable {
    // Time-to-live for failed transactions
    private static final long TRX_TTL_MILLIS = 900000; // 15 minutes
    private static final long TRX_CLEANUP_INTERVAL = 90000;
    private static final Logger LOG = LoggerFactory.getLogger(RemoteControl.class);
    private final DOMDataBroker domDataBroker;
    private final SchemaContext schemaContext;
    private final JsonConverter jsonConverter;
    private final ConcurrentMap<String, DataModificationContext> txmap = Maps.newConcurrentMap();
    private final ReadWriteLock trxGuard = new ReentrantReadWriteLock();
    private final Future<?> cleanerFuture;
    private final TransactionFactory transactionFactory;
    private final DataChangeListenerRegistry dataChangeRegistry;
    private final ScheduledExecutorService scheduledExecutorService;

    public RemoteControl(@NonNull final DOMDataBroker domDataBroker, @NonNull final SchemaContext schemaContext,
            TransportFactory transportFactory) {
        this(domDataBroker, schemaContext, TRX_CLEANUP_INTERVAL, transportFactory);
    }

    public RemoteControl(@NonNull final DOMDataBroker domDataBroker, @NonNull final SchemaContext schemaContext,
            long cleanupIntervalMilliseconds, @NonNull TransportFactory transportFactory) {
        this.domDataBroker = Objects.requireNonNull(domDataBroker);
        this.schemaContext = Objects.requireNonNull(schemaContext);
        this.jsonConverter = new JsonConverter(schemaContext);
        scheduledExecutorService = Executors.newScheduledThreadPool(1,
                new ThreadFactoryBuilder().setNameFormat("jsonrpc-tx-cleaner-%d").setDaemon(true).build());
        cleanerFuture = scheduledExecutorService.scheduleAtFixedRate(this::cleanupStaleTransactions,
                cleanupIntervalMilliseconds, cleanupIntervalMilliseconds, TimeUnit.MILLISECONDS);
        this.transactionFactory = new EnsureParentTransactionFactory(domDataBroker,
                Objects.requireNonNull(schemaContext));
        this.dataChangeRegistry = new DataChangeListenerRegistry(domDataBroker, transportFactory, jsonConverter);
    }

    /**
     * Remove stale transactions.To be eligible for removal, transaction must.
     * <ol>
     * <li>encountered error(s)</li>
     * <li>exists for at least TXR_TTL_MILLIS in map of transactions</li>
     * </ol>
     */
    private void cleanupStaleTransactions() {
        final long now = System.currentTimeMillis();
        txmap.entrySet().removeIf(e -> !e.getValue().isSuccess() && e.getValue().getCompletionTimestamp() != -1L
                && e.getValue().getCompletionTimestamp() + TRX_TTL_MILLIS > now);
    }

    @VisibleForTesting
    boolean isTxMapEmpty() {
        return txmap.entrySet().isEmpty();
    }

    @VisibleForTesting
    YangInstanceIdentifier path2II(JsonElement path) {
        return YangInstanceIdentifierDeserializer.toYangInstanceIdentifier(path, schemaContext);
    }

    @Override
    public JsonElement read(int store, String entity, JsonElement path) {
        final YangInstanceIdentifier pathAsIId = path2II(path);
        LOG.debug("READ : YII :{}", pathAsIId);
        try {
            final DOMDataTreeReadWriteTransaction rTrx = domDataBroker.newReadWriteTransaction();
            final NormalizedNode<?, ?> result = rTrx.read(int2store(store), pathAsIId).get().orElse(null);
            LOG.info("Result is {}", result);
            return jsonConverter.toBus(pathAsIId, result).getData();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Read failed", e);
        }
    }

    @Override
    public JsonElement read(String store, String entity, JsonElement path) {
        return read(Util.store2int(store), entity, path);
    }

    @Override
    public void put(String txId, int store, String entity, JsonElement path, JsonElement data) {
        final YangInstanceIdentifier pathAsIId = path2II(path);
        LOG.info("PUT txId : {}, store : {}, entity : {}, path : {}, YII :{}, data : {}", txId, int2store(store),
                entity, path, pathAsIId, data);
        final DOMDataTreeWriteTransaction wtx = allocateTrx(txId).getValue().newWriteTransaction();
        wtx.put(int2store(store), pathAsIId,
                jsonConverter.jsonElementToNormalizedNode(injectQName(pathAsIId, data), pathAsIId));
    }

    @Override
    public void put(String txId, String store, String entity, JsonElement path, JsonElement data) {
        put(txId, Util.store2int(store), entity, path, data);
    }

    @Override
    public boolean exists(int store, String entity, JsonElement path) {
        final YangInstanceIdentifier pathAsIId = path2II(path);
        LOG.debug("EXISTS store={}, entity={}, path={}, YII={}", int2store(store), entity, path, pathAsIId);
        try (DOMDataTreeReadTransaction trx = domDataBroker.newReadOnlyTransaction()) {
            return trx.exists(int2store(store), pathAsIId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Read failed", e);
        }
    }

    /**
     * Overloaded version of {@link #exists(int, String, JsonElement)}.
     */
    @Override
    public boolean exists(String store, String entity, JsonElement path) {
        return exists(Util.store2int(store), entity, path);
    }

    @Override
    public void merge(String txId, int store, String entity, JsonElement path, JsonElement data) {
        final DOMDataTreeWriteTransaction trx = allocateTrx(txId).getValue().newWriteTransaction();
        final YangInstanceIdentifier pathAsIId = path2II(path);
        LOG.debug("MERGE : tx={}, store={}, entity={}, path={}, YII={}, data={}", txId, int2store(store), entity, path,
                pathAsIId, data);
        trx.merge(int2store(store), pathAsIId, jsonConverter.jsonElementToNormalizedNode(data, pathAsIId, true));
    }

    /**
     * Overloaded version of
     * {@link #merge(String, int, String, JsonElement, JsonElement)}.
     */
    @Override
    public void merge(String txId, String store, String entity, JsonElement path, JsonElement data) {
        merge(txId, Util.store2int(store), entity, path, data);
    }

    @Override
    public void delete(String txId, int store, String entity, JsonElement path) {
        final YangInstanceIdentifier pathAsIId = path2II(path);
        LOG.debug("DELETE : tx={}, store={}, entity={}, path={}, YII={}", txId, int2store(store), entity, path,
                pathAsIId);
        final DOMDataTreeWriteTransaction trx = allocateTrx(txId).getValue().newWriteTransaction();
        trx.delete(int2store(store), pathAsIId);
    }

    /**
     * Overloaded version of {@link #delete(String, int, String, JsonElement)}.
     */
    @Override
    public void delete(String txId, String store, String entity, JsonElement path) {
        delete(txId, Util.store2int(store), entity, path);
    }

    @GuardedBy("trxGuard")
    @Override
    public boolean commit(String txId) {
        LOG.debug("COMMIT : {}", txId);
        final Lock lock = trxGuard.writeLock();
        try {
            lock.lock();
            if (!txmap.containsKey(txId)) {
                return false;
            }
            boolean succeed = txmap.get(txId).submit();
            if (succeed) {
                txmap.remove(txId);
            }
            return succeed;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean cancel(String txId) {
        LOG.debug("CANCEL : {}", txId);
        final DataModificationContext rwTx = txmap.remove(txId);
        return rwTx != null ? rwTx.cancel() : false;
    }

    @Override
    public String txid() {
        final String ret = allocateTrx(null).getKey();
        LOG.debug("TXID : {}", ret);
        return ret;
    }

    @Override
    public List<String> error(String txId) {
        LOG.debug("ERROR : {}", txId);
        if (txmap.containsKey(txId) && !txmap.get(txId).isSuccess()) {
            return txmap.get(txId).getErrors().stream().map(RemoteControl::serializeError).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Helper method to achieve compatibility between JSON-RPC data spec and
     * <a href="https://tools.ietf.org/html/rfc7951">RFC-7951</a><br />
     * <strong>Incompatibility example:</strong> YANG model (in module
     * 'test-model'):
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
     * This breaks requirement of GSON codec, which needs data to be qualified
     * by module name. Injecting/wrapping qualifier around it can workaround
     * this issue, so outcome of this method will be such as:
     *
     * <pre>
     *  {"test-model:grillconf":{"gasKnob":5}}
     * </pre>
     *
     * @param yii {@link YangInstanceIdentifier} of container node
     * @param inJson input JSON to be wrapped with qualifier
     * @return wrapped JSON with injected qualifier
     */
    @VisibleForTesting
    JsonElement injectQName(YangInstanceIdentifier yii, JsonElement inJson) {
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
        LOG.info("Wrapped data : {}", wrapper);
        return wrapper;
    }

    /**
     * Extract error messages from {@link Throwable}.
     *
     * @param error instance of {@link Throwable}
     * @return formated error message
     */
    private static String serializeError(final Throwable error) {
        final StringBuilder sb = new StringBuilder();
        Throwable cause = error;
        while (cause != null) {
            sb.append(cause.getMessage());
            cause = cause.getCause();
            if (cause != null) {
                sb.append(" : ");
            }
        }
        return sb.toString();
    }

    /*
     * Allocates new transaction and associate it to given UUID. If UUID is
     * NULL, then it's attempted to create random UUID few times. If it fails,
     * exception is thrown. By fact that UUID#randomUUID() uses
     * cryptographically strong PRNG, such situation is almost hypothetical only
     */
    @GuardedBy("trxGuard")
    private Entry<String, DataModificationContext> allocateTrx(String txId) {
        final Entry<String, DataModificationContext> ret;
        UUID uuid;
        // ConcurrentMap is thread-safe, but 2 operations on it are not atomic
        // as a whole, so use explicit lock
        final Lock lock = trxGuard.writeLock();
        try {
            lock.lock();
            // create new TX id and check if such ID exists
            if (txId == null) {
                uuid = UUID.randomUUID();
                ret = allocateTransactionInternal(uuid);
            } else {
                uuid = UUID.fromString(txId);
                if (txmap.containsKey(uuid.toString())) {
                    // given UUID already exists, return it
                    ret = new AbstractMap.SimpleEntry<>(txId, txmap.get(txId));
                } else {
                    // Create new transaction and put it into map
                    ret = allocateTransactionInternal(uuid);
                }
            }
            return ret;
        } finally {
            lock.unlock();
        }
    }

    private Entry<String, DataModificationContext> allocateTransactionInternal(UUID uuid) {
        final Entry<String, DataModificationContext> ret;
        final DataModificationContext ctx = new DataModificationContext(transactionFactory);
        ret = new AbstractMap.SimpleEntry<>(uuid.toString(), ctx);
        txmap.put(uuid.toString(), ctx);
        return ret;
    }

    @Override
    public void close() {
        scheduledExecutorService.shutdown();
        cleanerFuture.cancel(true);
        txmap.clear();
        dataChangeRegistry.close();
    }

    @Override
    public ListenerKey addListener(int store, String entity, JsonElement path) throws IOException {
        return addListener(store, entity, path, null);
    }

    @Override
    public ListenerKey addListener(String store, String entity, JsonElement path) throws IOException {
        return addListener(Util.store2int(store), entity, path);
    }

    @Override
    public ListenerKey addListener(String store, String entity, JsonElement path, String transport) throws IOException {
        return addListener(Util.store2int(store), entity, path, transport);
    }

    @Override
    public ListenerKey addListener(int store, String entity, JsonElement path, String transport) throws IOException {
        return dataChangeRegistry.createListener(path2II(path), Util.int2store(store), transport);
    }

    @Override
    public boolean deleteListener(String uri, String name) {
        return dataChangeRegistry.removeListener(uri, name);
    }
}
