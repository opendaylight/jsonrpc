/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import static org.opendaylight.jsonrpc.provider.common.Util.populateFromEndpointList;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonElement;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
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
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common code for peer context.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 11, 2020
 */
@Beta
@SuppressFBWarnings("SLF4J_LOGGER_SHOULD_BE_PRIVATE")
public abstract class AbstractPeerContext implements AutoCloseable {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractPeerContext.class);
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
        final DataObjectIdentifier<ActualEndpoints> peerOpId = DataObjectIdentifier.builder(Config.class)
                .child(ActualEndpoints.class, new ActualEndpointsKey(peer.requireName()))
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
            }
        }, MoreExecutors.directExecutor());
    }

    protected void publishState(ActualEndpointsBuilder builder, MountStatus status) {
        publishState(builder, status, Optional.empty(), null);
    }

    protected void publishState(ActualEndpointsBuilder builder, MountStatus status, Optional<Throwable> cause) {
        publishState(builder, status, cause, null);
    }

    protected void publishState(ActualEndpointsBuilder builder, MountStatus status, Optional<Throwable> cause,
            String managedBy) {
        final DataObjectIdentifier<ActualEndpoints> peerId = DataObjectIdentifier.builder(Config.class)
                .child(ActualEndpoints.class, new ActualEndpointsKey(peer.requireName()))
                .build();
        builder.setManagedBy(managedBy);
        builder.setMountStatus(status);
        cause.ifPresent(failure -> builder.setFailureReason(failure.getMessage()));
        final WriteTransaction wrTrx = dataBroker.newWriteOnlyTransaction();
        final ActualEndpoints opState = builder.build();
        wrTrx.merge(LogicalDatastoreType.OPERATIONAL, peerId, opState);
        LOG.debug("Changing op state to {}", opState);
        commitTransaction(wrTrx, peer.getName(), "Publish " + status + " state");
    }

    protected static void populatePathMap(HierarchicalEnumMap<JsonElement, DataType, String> pathMap, Peer peer) {
        populateFromEndpointList(pathMap, peer.nonnullDataConfigEndpoints().values(), DataType.CONFIGURATION_DATA);
        populateFromEndpointList(pathMap, peer.nonnullDataOperationalEndpoints().values(), DataType.OPERATIONAL_DATA);
        populateFromEndpointList(pathMap, peer.nonnullRpcEndpoints().values(), DataType.RPC);
        populateFromEndpointList(pathMap, peer.nonnullNotificationEndpoints().values(), DataType.NOTIFICATION);
    }

    public Peer getPeer() {
        return peer;
    }
}
