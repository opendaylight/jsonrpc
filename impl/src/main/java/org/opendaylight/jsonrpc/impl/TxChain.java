/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.jsonrpc.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;

import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.TransactionListener;
import org.opendaylight.mdsal.common.api.TransactionChainClosedException;
import org.opendaylight.mdsal.common.api.TransactionChainListener;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DOMTransactionChain} implementation for Netconf connector.
 */
public class TxChain extends AbstractJsonRPCComponent implements DOMTransactionChain, TransactionListener {
    private static final Logger LOG = LoggerFactory.getLogger(TxChain.class);
    private final DOMDataBroker dataBroker;
    private final TransactionChainListener listener;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * Transaction created by this chain that hasn't been submitted or cancelled
     * yet.
     */
    private JsonRPCTx currentTransaction = null;
    private volatile boolean closed = false;
    private volatile boolean successful = true;
    @GuardedBy("rwLock")
    private final ConcurrentMap<DOMDataTreeWriteTransaction, AutoCloseable> pendingTransactions =
        Maps.newConcurrentMap();

    public TxChain(@Nonnull final DOMDataBroker dataBroker, @Nonnull final TransactionChainListener listener,
            @Nonnull TransportFactory transportFactory,
            @Nonnull HierarchicalEnumMap<JsonElement, DataType, String> pathMap, @Nonnull JsonConverter jsonConverter,
            @Nonnull SchemaContext schemaContext) {
        super(schemaContext, transportFactory, pathMap, jsonConverter);
        this.dataBroker = Objects.requireNonNull(dataBroker);
        this.listener = Objects.requireNonNull(listener);
    }

    @Override
    public synchronized DOMDataTreeReadTransaction newReadOnlyTransaction() {
        checkOperationPermitted();
        return dataBroker.newReadOnlyTransaction();
    }

    private JsonRPCTx getWriteTransaction() {
        checkOperationPermitted();
        final Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            final DOMDataTreeWriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
            Preconditions.checkState(writeTransaction instanceof JsonRPCTx);
            final JsonRPCTx pendingWriteTx = (JsonRPCTx) writeTransaction;
            pendingTransactions.put(pendingWriteTx, pendingWriteTx.addCallback(this));
            currentTransaction = pendingWriteTx;
            return pendingWriteTx;
        } finally {
            lock.unlock();
        }
    }

    public JsonRPCTx newWriteOnlyTransaction() {
        return getWriteTransaction();
    }

    @Override
    public JsonRPCTx newReadWriteTransaction() {
        return getWriteTransaction();
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            notifyChainListenerSuccess();
        }
    }

    /**
     * Checks, if chain isn't closed and if there is no not submitted write
     * transaction waiting.
     */
    private void checkOperationPermitted() {
        if (closed) {
            throw new TransactionChainClosedException("Transaction chain was closed");
        }
        Preconditions.checkState(Objects.isNull(currentTransaction), "Last write transaction has not finished yet");
    }

    private void notifyChainListenerSuccess() {
        if (closed && successful && pendingTransactions.isEmpty()) {
            listener.onTransactionChainSuccessful(this);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void removeTx(final JsonRPCTx tx) {
        final Lock lock = rwLock.writeLock();
        // we need to make sure that returned value
        // from List#remove does not cause NPE
        try {
            lock.lock();
            Optional.fromNullable(pendingTransactions.remove(tx)).or(() -> {
                // NOOP
            }).close();
        } catch (Exception e) {
            LOG.error("Failed to remove pending transaction {}", tx, e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onCancel(final JsonRPCTx jsonRPCTx) {
        removeTx(jsonRPCTx);
        currentTransaction = null;
    }

    @Override
    public void onSuccess(JsonRPCTx tx) {
        removeTx(tx);
        notifyChainListenerSuccess();
    }

    @Override
    public void onFailure(JsonRPCTx tx, Throwable failure) {
        removeTx(tx);
        successful = false;
        if (currentTransaction != null) {
            currentTransaction.cancel();
        }
        listener.onTransactionChainFailed(this, tx, failure);
    }

    @Override
    public void onSubmit(JsonRPCTx jsonRPCTx) {
        currentTransaction = null;
    }
}
