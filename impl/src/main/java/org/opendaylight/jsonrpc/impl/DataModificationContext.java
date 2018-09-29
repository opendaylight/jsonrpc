/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.opendaylight.jsonrpc.model.TransactionFactory;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;

public class DataModificationContext implements AutoCloseable {
    private List<Throwable> errors = Collections.emptyList();
    private final List<DOMDataTreeWriteTransaction> txs = new ArrayList<>();
    private final AtomicLong completed = new AtomicLong(-1);

    public DataModificationContext(@Nonnull final TransactionFactory transactionFactory) {
        Objects.requireNonNull(transactionFactory);
        txs.add(transactionFactory.get());
    }

    /**
     * Allocates new transaction.
     *
     * @return {@link DOMDataTreeWriteTransaction}
     */
    public DOMDataTreeWriteTransaction newWriteTransaction() {
        return txs.get(0);
    }

    /**
     * Cancel all chained transactions.
     * @return true if no transaction has been allocated.
     */
    public boolean cancel() {
        try {
            if (txs.isEmpty()) {
                // there is nothing no cancel
                return true;
            }
            return txs.stream().allMatch(DOMDataTreeWriteTransaction::cancel);
        } finally {
            completed.set(System.currentTimeMillis());
        }
    }

    /**
     * Commits all chained transactions and collect any potential errors.
     *
     * @return true if and only if all transactions in chain succeeded
     * @see #isSuccess()
     */
    public boolean submit() {
        try {
            // there is nothing to submit
            if (txs.isEmpty()) {
                return false;
            }
            errors = txs.stream().map(DataModificationContext::extractError)
                    .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty()).collect(Collectors.toList());
            return errors.isEmpty();
        } finally {
            completed.set(System.currentTimeMillis());
        }
    }

    /**
     * Check if transaction chain processing succeeded.
     *
     * @return true if and only if all transactions in chain succeeded (no
     *         errors has been recorded)
     */
    public boolean isSuccess() {
        return errors.isEmpty();
    }

    /**
     * Returns the completion timestamp.
     *
     * @return completion timestamp in UTC.
     *
     * @see System#currentTimeMillis()
     */
    public long getCompletionTimestamp() {
        return completed.get();
    }

    /**
     * Adds {@link Throwable} into exception list.
     *
     * @param ex {@link Throwable} instance to add
     */
    public void addError(Throwable ex) {
        errors.add(ex);
    }

    /**
     * Returns any errors that occurred.
     *
     * @return immutable copy of errors, if no errors occurred, list is empty (never NULL).
     */
    @Nonnull
    public List<Throwable> getErrors() {
        return ImmutableList.copyOf(errors);
    }

    /*
     * Helper method which perform transaction submission and extract Exception
     */
    private static Optional<Throwable> extractError(DOMDataTreeWriteTransaction tx) {
        try {
            tx.commit().get();
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            return Optional.of(e);
        }
    }

    @Override
    public void close() {
        //NOOP
    }

    @Override
    public String toString() {
        return "DataModificationContext [errors=" + errors + ", txs=" + txs + "]";
    }
}
