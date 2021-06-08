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
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Objects;
import java.util.Optional;
import org.opendaylight.jsonrpc.provider.cluster.messages.PathAndDataMsg;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

/**
 * Implementation of {@link DOMDataTreeReadWriteTransaction} that interacts with an actor.
 *
 * <p>
 * Acknowledgement : this code is inspired by implementation of netconf-topology-singleton.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 11, 2020
 */
class ActorProxyTransaction implements ProxyTransactionFacade {
    private static final Logger LOG = LoggerFactory.getLogger(ActorProxyTransaction.class);

    private final ActorRef actorRef;
    private final Peer peer;
    private final ExecutionContext executionContext;
    private final Timeout askTimeout;
    private final String name;

    ActorProxyTransaction(final ActorRef actorRef, final Peer peer, final ExecutionContext executionContext,
            final Timeout askTimeout) {
        this.actorRef = Objects.requireNonNull(actorRef);
        this.peer = Objects.requireNonNull(peer);
        this.name = peer.getName();
        this.executionContext = Objects.requireNonNull(executionContext);
        this.askTimeout = Objects.requireNonNull(askTimeout);
    }

    @Override
    public Object getIdentifier() {
        return peer;
    }

    @Override
    public boolean cancel() {
        LOG.debug("[{}]: Cancel tx via actor {}", name, actorRef);
        final Future<Object> future = Patterns.ask(actorRef, new TxCancel(), askTimeout);
        future.onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object response) {
                if (failure != null) {
                    LOG.warn("[{}] tx cancel failed", name, failure);
                    return;
                }
                LOG.debug("[{}] tx cancel succeeded", name);
            }
        }, executionContext);
        return true;
    }

    @Override
    public FluentFuture<Optional<NormalizedNode>> read(final LogicalDatastoreType store,
            final YangInstanceIdentifier path) {
        LOG.debug("[{}] Read {} {} via actor {}", name, store, path, actorRef);
        final Future<Object> future = Patterns.ask(actorRef, new TxRead(store, path, false), askTimeout);
        final SettableFuture<Optional<NormalizedNode>> settableFuture = SettableFuture.create();
        future.onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object response) {
                if (failure != null) {
                    LOG.debug("[{}]: Read {} {} failed", name, store, path, failure);
                    if (failure instanceof ReadFailedException) {
                        settableFuture.setException(failure);
                    } else {
                        settableFuture.setException(
                                new ReadFailedException("Read of store " + store + " at " + path + " failed", failure));
                    }
                    return;
                }

                LOG.debug("[{}] Read {} {} succeeded: {}", name, store, path, response);

                if (response instanceof EmptyReadResponse) {
                    settableFuture.set(Optional.empty());
                } else if (response instanceof PathAndDataMsg) {
                    final PathAndDataMsg pad = (PathAndDataMsg) response;
                    settableFuture.set(Optional.of(pad.getData()));
                }
            }
        }, executionContext);

        return FluentFuture.from(settableFuture);
    }

    @Override
    public FluentFuture<Boolean> exists(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        LOG.debug("[{}] Exists {} {} via actor {}", name, store, path, actorRef);
        final SettableFuture<Boolean> settableFuture = SettableFuture.create();
        Patterns.ask(actorRef, new TxRead(store, path, true), askTimeout).onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object response) {
                if (failure != null) {
                    LOG.debug("[{}] Exists {} {} failed", name, store, path, failure);
                    if (failure instanceof ReadFailedException) {
                        settableFuture.setException(failure);
                    } else {
                        settableFuture.setException(new ReadFailedException(
                                "Exists of store " + store + " path " + path + " failed", failure));
                    }
                    return;
                }

                LOG.debug("[{}] Exists {} {} succeeded: {}", name, store, path, response);
                settableFuture.set((Boolean) response);
            }
        }, executionContext);

        return FluentFuture.from(settableFuture);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        LOG.debug("[{}] Delete {} {} via actor {}", name, store, path, actorRef);
        actorRef.tell(new TxDelete(store, path), ActorRef.noSender());
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode data) {
        LOG.debug("[{}] Put {} {} via actor {}", name, store, path, actorRef);
        actorRef.tell(new TxPut(store, new PathAndDataMsg(path, data)), ActorRef.noSender());
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode data) {
        LOG.debug("[{}] Merge {} {} via actor {}", name, store, path, actorRef);
        actorRef.tell(new TxMerge(store, new PathAndDataMsg(path, data)), ActorRef.noSender());
    }

    @Override
    public FluentFuture<? extends CommitInfo> commit() {
        LOG.debug("[{}] Commit via actor {}", name, actorRef);
        final SettableFuture<CommitInfo> settableFuture = SettableFuture.create();
        Patterns.ask(actorRef, new TxCommit(), askTimeout).onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object response) {
                if (failure != null) {
                    LOG.debug("[{}] Commit failed", name, failure);
                    settableFuture.setException(newTransactionCommitFailedException(failure));
                    return;
                }
                LOG.debug("[{}] Commit succeeded", name);
                settableFuture.set(CommitInfo.empty());
            }

            private TransactionCommitFailedException newTransactionCommitFailedException(final Throwable failure) {
                return new TransactionCommitFailedException(
                        String.format("%s: Commit of transaction failed", getIdentifier()), failure);
            }
        }, executionContext);

        return FluentFuture.from(settableFuture);
    }
}
