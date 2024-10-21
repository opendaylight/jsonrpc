/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.tx;

import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.Status.Failure;
import akka.actor.Status.Success;
import akka.actor.UntypedAbstractActor;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Optional;
import org.opendaylight.jsonrpc.provider.cluster.messages.PathAndDataMsg;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 * Actor to interact with Peer's {@link DOMDataBroker}.
 *
 * <p>Acknowledgement : this code is inspired by netconf-topology-singleton implementation.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 12, 2020
 */
public final class TransactionActor extends UntypedAbstractActor {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionActor.class);
    private final DOMDataTreeReadWriteTransaction tx;
    private final long idleTimeout;

    private TransactionActor(final DOMDataTreeReadWriteTransaction tx, final Duration idleTimeout) {
        this.tx = tx;
        this.idleTimeout = idleTimeout.toSeconds();
        if (this.idleTimeout > 0) {
            context().setReceiveTimeout(idleTimeout);
        }
    }

    public static Props props(final DOMDataTreeReadWriteTransaction tx, final Duration idleTimeout) {
        return Props.create(TransactionActor.class, () -> new TransactionActor(tx, idleTimeout));
    }

    @Override
    public void onReceive(final Object message) {
        if (message instanceof TxRead) {
            final TxRead readRequest = (TxRead) message;
            final YangInstanceIdentifier path = readRequest.getPath();
            final LogicalDatastoreType store = readRequest.getStore();
            if (readRequest.isExists()) {
                exists(path, store);
            } else {
                read(path, store);
            }
        } else if (message instanceof TxMerge) {
            final TxMerge mergeRequest = (TxMerge) message;
            final PathAndDataMsg data = mergeRequest.getMessage();
            tx.merge(mergeRequest.getStore(), data.getPath(), data.getData());
        } else if (message instanceof TxPut) {
            final TxPut putRequest = (TxPut) message;
            final PathAndDataMsg data = putRequest.getMessage();
            tx.put(putRequest.getStore(), data.getPath(), data.getData());
        } else if (message instanceof TxDelete) {
            final TxDelete deleteRequest = (TxDelete) message;
            tx.delete(deleteRequest.getStore(), deleteRequest.getPath());
        } else if (message instanceof TxCancel) {
            cancel();
        } else if (message instanceof TxCommit) {
            commit();
        } else if (message instanceof ReceiveTimeout) {
            LOG.warn("Haven't received any message for {} seconds, cancelling transaction and stopping actor",
                    idleTimeout);
            tx.cancel();
            context().stop(self());
        } else {
            unhandled(message);
        }
    }

    private void read(final YangInstanceIdentifier path, final LogicalDatastoreType store) {
        tx.read(store, path).addCallback(new FutureCallback<Optional<NormalizedNode>>() {
            @Override
            public void onSuccess(final Optional<NormalizedNode> result) {
                if (!result.isPresent()) {
                    sender().tell(new EmptyReadResponse(), self());
                } else {
                    sender().tell(new PathAndDataMsg(path, result.orElseThrow()), self());
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                sender().tell(new Failure(throwable), self());
            }
        }, MoreExecutors.directExecutor());
    }

    private void exists(final YangInstanceIdentifier path, final LogicalDatastoreType store) {
        tx.exists(store, path).addCallback(new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(final Boolean result) {
                if (result == null) {
                    sender().tell(Boolean.FALSE, self());
                } else {
                    sender().tell(result, self());
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                sender().tell(new Failure(throwable), self());
            }
        }, MoreExecutors.directExecutor());
    }

    private void cancel() {
        final boolean cancelled = tx.cancel();
        sender().tell(cancelled, self());
        context().stop(self());
    }

    private void commit() {
        final FluentFuture<? extends CommitInfo> submitFuture = tx.commit();
        context().stop(self());
        submitFuture.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                sender().tell(new Success(null), self());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                sender().tell(new Failure(throwable), self());
            }
        }, MoreExecutors.directExecutor());
    }
}
