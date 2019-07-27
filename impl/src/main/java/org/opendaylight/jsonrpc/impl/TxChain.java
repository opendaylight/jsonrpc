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

import javax.annotation.concurrent.GuardedBy;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.JsonRpcTransactionFacade;
import org.opendaylight.jsonrpc.model.TransactionListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainClosedException;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.mdsal.dom.api.DOMTransactionFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DOMTransactionChain} implementation for Netconf connector.
 */
public class TxChain extends AbstractJsonRPCComponent implements DOMTransactionChain, TransactionListener {
    private static final Logger LOG = LoggerFactory.getLogger(TxChain.class);
    private final DOMTransactionFactory dataBroker;
    private final DOMTransactionChainListener listener;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * Transaction created by this chain that hasn't been submitted or cancelled
     * yet.
     */
    private DOMDataTreeWriteTransaction currentTransaction = null;
    private volatile boolean closed = false;
    private volatile boolean successful = true;
    @GuardedBy("rwLock")
    private final ConcurrentMap<DOMDataTreeWriteTransaction, AutoCloseable> pendingTransactions =
        Maps.newConcurrentMap();

    public TxChain(@NonNull final DOMTransactionFactory dataBroker, @NonNull final DOMTransactionChainListener listener,
            @NonNull TransportFactory transportFactory,
            @NonNull HierarchicalEnumMap<JsonElement, DataType, String> pathMap, @NonNull JsonConverter jsonConverter,
            @NonNull SchemaContext schemaContext, @NonNull Peer peer) {
        super(schemaContext, transportFactory, pathMap, jsonConverter, peer);
        this.dataBroker = Objects.requireNonNull(dataBroker);
        this.listener = Objects.requireNonNull(listener);
    }

    @Override
    public synchronized DOMDataTreeReadTransaction newReadOnlyTransaction() {
        checkOperationPermitted();
        return dataBroker.newReadOnlyTransaction();
    }

    private DOMDataTreeReadWriteTransaction getWriteTransaction() {
        checkOperationPermitted();
        final Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            final DOMDataTreeReadWriteTransaction writeTransaction = dataBroker.newReadWriteTransaction();
            Preconditions.checkState(writeTransaction instanceof JsonRpcTransactionFacade);
            final DOMDataTreeReadWriteTransaction pendingWriteTx = writeTransaction;
            pendingTransactions.put(pendingWriteTx, ((JsonRpcTransactionFacade) pendingWriteTx).addCallback(this));
            currentTransaction = pendingWriteTx;
            return pendingWriteTx;
        } finally {
            lock.unlock();
        }
    }

    public DOMDataTreeWriteTransaction newWriteOnlyTransaction() {
        return getWriteTransaction();
    }

    @Override
    public DOMDataTreeReadWriteTransaction newReadWriteTransaction() {
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
            throw new DOMTransactionChainClosedException("Transaction chain was closed");
        }
        Preconditions.checkState(Objects.isNull(currentTransaction), "Last write transaction has not finished yet");
    }

    private void notifyChainListenerSuccess() {
        if (closed && successful && pendingTransactions.isEmpty()) {
            listener.onTransactionChainSuccessful(this);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void removeTx(final DOMDataTreeWriteTransaction tx) {
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
    public void onCancel(final DOMDataTreeWriteTransaction jsonRPCTx) {
        removeTx(jsonRPCTx);
        currentTransaction = null;
    }

    @Override
    public void onSuccess(DOMDataTreeWriteTransaction tx) {
        removeTx(tx);
        notifyChainListenerSuccess();
    }

    @Override
    public void onFailure(DOMDataTreeWriteTransaction tx, Throwable failure) {
        removeTx(tx);
        successful = false;
        if (currentTransaction != null) {
            currentTransaction.cancel();
        }
        listener.onTransactionChainFailed(this, tx, failure);
    }

    @Override
    public void onSubmit(DOMDataTreeWriteTransaction jsonRPCTx) {
        currentTransaction = null;
    }
}
