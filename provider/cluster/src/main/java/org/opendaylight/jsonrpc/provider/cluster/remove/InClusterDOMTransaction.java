/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.remove;

import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.provider.cluster.messages.PathAndDataMsg;
import org.opendaylight.jsonrpc.provider.cluster.messages.TxCancel;
import org.opendaylight.jsonrpc.provider.cluster.messages.TxDelete;
import org.opendaylight.jsonrpc.provider.cluster.messages.TxMerge;
import org.opendaylight.jsonrpc.provider.cluster.messages.TxPut;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;

public class InClusterDOMTransaction implements DOMDataTreeReadWriteTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(InClusterDOMTransaction.class);

    private final ActorRef masterActor;
    private final String id;
    private final ExecutionContext executionContext;
    private final Timeout askTimeout;

    public InClusterDOMTransaction(ActorRef masterActor, String id, ExecutionContext executionContext, Timeout askTimeout) {
        this.masterActor = masterActor;
        this.id = id;
        this.askTimeout = askTimeout;
        this.executionContext = executionContext;
    }

    @Override
    public @NonNull FluentFuture<? extends @NonNull CommitInfo> commit() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean cancel() {
        LOG.debug("[cancel][{}] => {}", id, masterActor);
        Patterns.ask(masterActor, new TxCancel(), askTimeout).onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object response) {
                if (failure != null) {
                    LOG.warn("[cancel][{}] : failure", id, failure);
                    return;
                }
                LOG.debug("[cancel][{}] : success", id);
            }
        }, executionContext);
        return true;
    }

    @Override
    public @NonNull Object getIdentifier() {
        return id;
    }

    @Override
    public void put(LogicalDatastoreType store, YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        LOG.debug("[put][{}] => {} {} {}", id, masterActor, store, path);
        masterActor.tell(new TxPut(store, new PathAndDataMsg(path, data)), ActorRef.noSender());
    }

    @Override
    public void merge(LogicalDatastoreType store, YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        LOG.debug("[merge][{}] => {} : {} {}", id, masterActor, store, path);
        masterActor.tell(new TxMerge(store, new PathAndDataMsg(path, data)), ActorRef.noSender());
    }

    @Override
    public void delete(LogicalDatastoreType store, YangInstanceIdentifier path) {
        LOG.debug("[delete][{}] => {} : {} {}", id, masterActor, store, path);
        masterActor.tell(new TxDelete(store, path), ActorRef.noSender());
    }

    @Override
    public FluentFuture<Optional<NormalizedNode<?, ?>>> read(LogicalDatastoreType store, YangInstanceIdentifier path) {
        LOG.info("[read][{}] => {} : {} {}", id, masterActor, store, path);
        return null;
    }

    @Override
    public FluentFuture<Boolean> exists(LogicalDatastoreType store, YangInstanceIdentifier path) {
        // TODO Auto-generated method stub
        return null;
    }

}
