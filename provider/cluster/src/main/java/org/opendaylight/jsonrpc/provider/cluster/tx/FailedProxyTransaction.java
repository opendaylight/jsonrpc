/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.tx;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FailedProxyTransaction implements ProxyTransactionFacade {
    private static final Logger LOG = LoggerFactory.getLogger(FailedProxyTransaction.class);
    private final String name;
    private final Throwable failure;

    FailedProxyTransaction(final String name, final Throwable failure) {
        this.name = Objects.requireNonNull(name);
        this.failure = Objects.requireNonNull(failure);
    }

    @Override
    public Object getIdentifier() {
        return name;
    }

    @Override
    public FluentFuture<Optional<NormalizedNode>> read(final LogicalDatastoreType store,
            final YangInstanceIdentifier path) {
        LOG.debug("[{}][FAILED] Read {} {}", name, store, path, failure);
        return FluentFutures.immediateFailedFluentFuture(ReadFailedException.MAPPER
                .apply(failure instanceof Exception ? (Exception) failure : new ReadFailedException("read", failure)));
    }

    @Override
    public FluentFuture<Boolean> exists(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        LOG.debug("[{}][FAILED] Exists {} {}", name, store, path, failure);
        return FluentFutures.immediateFailedFluentFuture(ReadFailedException.MAPPER.apply(
                failure instanceof Exception ? (Exception) failure : new ReadFailedException("exists", failure)));
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        LOG.debug("[{}][FAILED] Delete {} {}", name, store, path, failure);
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
            final NormalizedNode data) {
        LOG.debug("[{}][FAILED] Put {} {} - failure", name, store, path, failure);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
            final NormalizedNode data) {
        LOG.debug("[{}][FAILED] Merge {} {}", name, store, path, failure);
    }

    @Override
    public boolean cancel() {
        LOG.debug("[{}][FAILED] Cancel", name, failure);
        return true;
    }

    @Override
    public @NonNull FluentFuture<? extends @NonNull CommitInfo> commit() {
        LOG.debug("[{}][FAILED] Commit", name, failure);
        final TransactionCommitFailedException cause;
        if (failure instanceof TransactionCommitFailedException) {
            cause = (TransactionCommitFailedException) failure;
        } else {
            cause = new TransactionCommitFailedException("commit", failure);
        }
        return FluentFutures.immediateFailedFluentFuture(cause);
    }
}