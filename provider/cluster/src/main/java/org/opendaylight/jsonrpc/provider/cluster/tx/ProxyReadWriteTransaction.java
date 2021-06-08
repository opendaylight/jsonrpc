/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.tx;

import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.SettableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

public class ProxyReadWriteTransaction implements DOMDataTreeReadWriteTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyReadWriteTransaction.class);
    private final String name;
    private final AtomicBoolean opened = new AtomicBoolean(true);
    private final List<Consumer<ProxyTransactionFacade>> queue = new ArrayList<>();
    private volatile ProxyTransactionFacade txFacade;

    public ProxyReadWriteTransaction(final Peer peer, final Future<Object> future,
            final ExecutionContext executionContext, final Timeout askTimeout) {
        this.name = peer.getName();

        future.onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object actorRef) {
                final ProxyTransactionFacade facade;
                if (failure != null) {
                    LOG.debug("[{}] Failed to obtain master actor", name, failure);
                    facade = new FailedProxyTransaction(name, failure);
                } else {
                    LOG.debug("[{}] Obtained master actor {}", name, actorRef);
                    facade = new ActorProxyTransaction((ActorRef) actorRef, peer, executionContext, askTimeout);
                }

                invokeBefore(facade);
            }
        }, executionContext);
    }

    @Override
    public boolean cancel() {
        if (!opened.compareAndSet(true, false)) {
            return false;
        }

        performAction(DOMDataTreeWriteTransaction::cancel, "cancel");
        return true;
    }

    @Override
    public FluentFuture<Optional<NormalizedNode>> read(final LogicalDatastoreType store,
            final YangInstanceIdentifier path) {
        LOG.debug("[{}] Read {} {}", name, store, path);
        final SettableFuture<Optional<NormalizedNode>> returnFuture = SettableFuture.create();
        performAction(facade -> returnFuture.setFuture(facade.read(store, path)), "read");
        return FluentFuture.from(returnFuture);
    }

    @Override
    public FluentFuture<Boolean> exists(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        LOG.debug("[{}] Exists {} {}", name, store, path);
        final SettableFuture<Boolean> returnFuture = SettableFuture.create();
        performAction(facade -> returnFuture.setFuture(facade.exists(store, path)), "exists");
        return FluentFuture.from(returnFuture);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        checkOpen();
        LOG.debug("[{}] Delete {} {}", name, store, path);
        performAction(facade -> facade.delete(store, path), "delete");
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode data) {
        checkOpen();
        LOG.debug("[{}] Put {} {}", name, store, path);
        performAction(facade -> facade.put(store, path, data), "put");
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode data) {
        checkOpen();
        LOG.debug("[{}] Merge {} {}", name, store, path);
        performAction(facade -> facade.merge(store, path, data), "merge");
    }

    @Override
    public @NonNull FluentFuture<? extends @NonNull CommitInfo> commit() {
        Preconditions.checkState(opened.compareAndSet(true, false), "[%s] Transaction is already closed", name);
        LOG.debug("[{}] Commit", name);

        final SettableFuture<CommitInfo> returnFuture = SettableFuture.create();
        performAction(facade -> returnFuture.setFuture(facade.commit()), "commit");
        return FluentFuture.from(returnFuture);
    }

    @Override
    public Object getIdentifier() {
        return this;
    }

    private void performAction(final Consumer<ProxyTransactionFacade> operation, String opDescription) {
        final ProxyTransactionFacade facadeOnEntry;
        synchronized (queue) {
            if (txFacade == null) {
                LOG.debug("[{}]: Queuing transaction operation '{}'", name, opDescription);
                queue.add(operation);
                facadeOnEntry = null;
            } else {
                facadeOnEntry = txFacade;
            }
        }

        if (facadeOnEntry != null) {
            operation.accept(facadeOnEntry);
        }
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private void invokeBefore(final ProxyTransactionFacade newTransactionFacade) {
        while (true) {
            final Collection<Consumer<ProxyTransactionFacade>> operationsBatch;
            synchronized (queue) {
                if (queue.isEmpty()) {
                    txFacade = newTransactionFacade;
                    break;
                }
                operationsBatch = new ArrayList<>(queue);
                queue.clear();
            }
            for (Consumer<ProxyTransactionFacade> oper : operationsBatch) {
                oper.accept(newTransactionFacade);
            }
        }
    }

    private void checkOpen() {
        Preconditions.checkState(opened.get(), "%s: Transaction is closed", name);
    }
}
