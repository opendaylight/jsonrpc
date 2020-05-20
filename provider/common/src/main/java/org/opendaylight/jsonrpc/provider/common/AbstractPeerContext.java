/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Objects;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.MountStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpointsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common code for peer context.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 11, 2020
 */
@Beta
public abstract class AbstractPeerContext implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPeerContext.class);
    protected final Peer peer;
    protected final DataBroker dataBroker;

    public AbstractPeerContext(final Peer peer, final DataBroker dataBroker) {
        this.peer = Objects.requireNonNull(peer);
        this.dataBroker = Objects.requireNonNull(dataBroker);
    }

    @Override
    public void close() {
        removeOperationalState();
    }

    protected void removeOperationalState() {
        final WriteTransaction wrTrx = dataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<ActualEndpoints> peerOpId = InstanceIdentifier.builder(Config.class)
                .child(ActualEndpoints.class, new ActualEndpointsKey(peer.getName()))
                .build();
        wrTrx.delete(LogicalDatastoreType.OPERATIONAL, peerOpId);
        commitTransaction(wrTrx, peer.getName(), "Unpublish operational state");
    }

    /*
     * Commit a transaction to datastore
     */
    protected void commitTransaction(final WriteTransaction transaction, final String device, final String txType) {
        LOG.trace("{}: Committing Transaction {}:{}", device, txType, transaction.getIdentifier());
        transaction.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo info) {
                LOG.trace("{}: Transaction({}) SUCCESSFUL", txType, transaction.getIdentifier());
            }

            @Override
            public void onFailure(final Throwable failure) {
                LOG.error("{}: Transaction({}) FAILED!", txType, transaction.getIdentifier(), failure);
                throw new IllegalStateException(
                        String.format("%s : Transaction(%s) not commited currectly", device, txType), failure);
            }
        }, MoreExecutors.directExecutor());
    }

    protected void publishState(ActualEndpointsBuilder builder, MountStatus status, Optional<Throwable> cause) {
        final InstanceIdentifier<ActualEndpoints> peerId = InstanceIdentifier.builder(Config.class)
                .child(ActualEndpoints.class, new ActualEndpointsKey(peer.getName()))
                .build();
        builder.setMountStatus(status);
        cause.ifPresent(failure -> builder.setFailureReason(failure.getMessage()));
        final WriteTransaction wrTrx = dataBroker.newWriteOnlyTransaction();
        wrTrx.merge(LogicalDatastoreType.OPERATIONAL, peerId, builder.build());
        commitTransaction(wrTrx, peer.getName(), "Publish " + status + " state");
    }
}
