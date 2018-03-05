/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonElement;
import java.net.URISyntaxException;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumHashMap;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.hmap.JsonPathCodec;
import org.opendaylight.jsonrpc.model.MutablePeer;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.model.SchemaContextProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataConfigEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataOperationalEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.NotificationEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpointsBuilder;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context of mapped {@link Peer}.
 *
 * <p>
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class MappedPeerContext implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(MappedPeerContext.class);
    private final Peer peer;
    private final HierarchicalEnumMap<JsonElement, DataType, String> pathMap = HierarchicalEnumHashMap
            .create(DataType.class, JsonPathCodec.create());
    private final JsonRPCtoRPCBridge rpcBridge;
    private final JsonRPCDataBroker rpcDataBroker;
    private final JsonRPCNotificationService notificationService;
    private final ObjectRegistration<DOMMountPoint> mountpointRegistration;
    private final DataBroker dataBroker;
    private final YangInstanceIdentifier biPath;

    public MappedPeerContext(@Nonnull Peer peer, @Nonnull TransportFactory transportFactory,
            @Nonnull SchemaContextProvider schemaContextProvider, @Nonnull DataBroker dataBroker,
            @Nonnull DOMMountPointService mountService, @Nullable RemoteGovernance governance)
            throws URISyntaxException {
        this.peer = Objects.requireNonNull(peer);
        this.dataBroker = Objects.requireNonNull(dataBroker);
        final MutablePeer newPeer = new MutablePeer().name(peer.getName()).addModels(peer.getModules());
        biPath = Util.createBiPath(peer.getName());

        final DOMMountPointService.DOMMountPointBuilder mountBuilder = mountService.createMountPoint(biPath);

        /*
         * We obtain models from the same source as the bus master to ensure
         * that everyone has a coherent view of the system using the same model
         * versions
         */
        final SchemaContext schema = schemaContextProvider.createSchemaContext(peer);

        mountBuilder.addInitialSchemaContext(schema);

        /*
         * DataBroker
         */
        rpcDataBroker = new JsonRPCDataBroker(peer, schema, pathMap, transportFactory, governance);
        mountBuilder.addService(DOMDataBroker.class, rpcDataBroker);

        pathMap.toMap(DataType.CONFIGURATION_DATA).entrySet().stream()
                .forEach(e -> newPeer.addDataConfigEndpoint(
                        new DataConfigEndpointsBuilder().setPath(e.getKey().getAsJsonObject().toString())
                                .setEndpointUri(new Uri(e.getValue())).build()));
        pathMap.toMap(DataType.OPERATIONAL_DATA).entrySet().stream()
                .forEach(e -> newPeer.addDataOperationalEndpoint(
                        new DataOperationalEndpointsBuilder().setPath(e.getKey().getAsJsonObject().toString())
                                .setEndpointUri(new Uri(e.getValue())).build()));

        /*
         * RPC bridge
         */
        rpcBridge = new JsonRPCtoRPCBridge(peer, schema, pathMap, governance, transportFactory);
        mountBuilder.addService(DOMRpcService.class, rpcBridge);
        pathMap.toMap(DataType.RPC).entrySet().stream().forEach(e -> newPeer.addRpcEndpoint(new RpcEndpointsBuilder()
                .setPath(e.getKey().getAsJsonObject().toString()).setEndpointUri(new Uri(e.getValue())).build()));

        /*
         * Notification service
         */
        notificationService = new JsonRPCNotificationService(peer, schema, pathMap, transportFactory, governance);
        pathMap.toMap(DataType.NOTIFICATION).entrySet().stream()
                .forEach(e -> newPeer.addNotificationEndpoint(
                        new NotificationEndpointsBuilder().setPath(e.getKey().getAsJsonObject().toString())
                                .setEndpointUri(new Uri(e.getValue())).build()));
        mountBuilder.addService(DOMNotificationService.class, notificationService);

        mountpointRegistration = mountBuilder.register();

        // Publish operational state
        final ActualEndpoints endpoint = new ActualEndpointsBuilder(newPeer).build();
        final InstanceIdentifier<ActualEndpoints> peerOpId = InstanceIdentifier.builder(Config.class)
                .child(ActualEndpoints.class, new ActualEndpointsKey(newPeer.getName())).build();
        final WriteTransaction wrTrx = dataBroker.newWriteOnlyTransaction();
        wrTrx.put(LogicalDatastoreType.OPERATIONAL, peerOpId, endpoint);
        commitTransaction(wrTrx, peer.getName(), "Publish operational state");
    }

    @Override
    public void close() throws Exception {
        Lists.<AutoCloseable>newArrayList(rpcDataBroker, rpcBridge, notificationService, mountpointRegistration)
                .stream().forEach(c -> Util.closeNullableWithExceptionCallback(c,
                    e -> LOG.error("Failed to close provider {}", c, e)));
        removeOperationalState();
    }

    private void removeOperationalState() {
        final WriteTransaction wrTrx = dataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<ActualEndpoints> peerOpId = InstanceIdentifier.builder(Config.class)
                .child(ActualEndpoints.class, new ActualEndpointsKey(peer.getName())).build();
        wrTrx.delete(LogicalDatastoreType.OPERATIONAL, peerOpId);
        commitTransaction(wrTrx, getName(), "Unpublish operational state");
    }

    /*
     * Commit a transaction to datastore
     */
    private void commitTransaction(final WriteTransaction transaction, final String device, final String txType) {
        LOG.trace("{}: Committing Transaction {}:{}", device, txType, transaction.getIdentifier());
        final CheckedFuture<Void, TransactionCommitFailedException> result = transaction.submit();

        Futures.addCallback(result, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void noop) {
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

    public String getName() {
        return peer.getName();
    }

    @Override
    public String toString() {
        return "MappedPeerContext [peer=" + peer.getName() + ", path = " + biPath + "]";
    }
}
