/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonElement;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumHashMap;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.hmap.JsonPathCodec;
import org.opendaylight.jsonrpc.model.MutablePeer;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPoint;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
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
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context of mapped {@link Peer}.
 *
 * <p>
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public class MappedPeerContext implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(MappedPeerContext.class);
    private final Peer peer;
    private final HierarchicalEnumMap<JsonElement, DataType, String> pathMap = HierarchicalEnumHashMap
            .create(DataType.class, JsonPathCodec.create());
    private final JsonConverter jsonConverter;
    private final JsonRPCtoRPCBridge rpcBridge;
    private final JsonRPCDataBroker rpcDataBroker;
    private final JsonRPCNotificationService notificationService;
    private final ObjectRegistration<DOMMountPoint> mountpointRegistration;
    private final DataBroker dataBroker;
    private final YangInstanceIdentifier biPath;

    public MappedPeerContext(@NonNull Peer peer, @NonNull TransportFactory transportFactory,
            @NonNull DOMSchemaService schemaService, @NonNull DataBroker dataBroker,
            @NonNull DOMMountPointService mountService, @Nullable RemoteGovernance governance)
            throws URISyntaxException {
        this.peer = Objects.requireNonNull(peer);
        this.dataBroker = Objects.requireNonNull(dataBroker);
        final MutablePeer newPeer = new MutablePeer().name(peer.getName());
        final SchemaContext schema;

        //check if peer can supply modules by himself
        if (supportInbandModels(peer)) {
            schema = InbandModelsSchemaContextProvider.create(transportFactory).createSchemaContext(peer);
            //actual list of modules lies in created SchemaContext
            newPeer.addModels(schema.getModules()
                    .stream()
                    .map(m -> new YangIdentifier(m.getName()))
                    .collect(Collectors.toList()));
        } else {
            /*
             * We obtain models from the same source as the bus master to ensure
             * that everyone has a coherent view of the system using the same model
             * versions
             */
            schema = governance != null ? new GovernanceSchemaContextProvider(governance).createSchemaContext(peer)
                    : new BuiltinSchemaContextProvider(schemaService.getGlobalContext()).createSchemaContext(peer);
        }

        biPath = Util.createBiPath(peer.getName());

        final DOMMountPointService.DOMMountPointBuilder mountBuilder = mountService.createMountPoint(biPath);
        jsonConverter = new JsonConverter(schema);
        mountBuilder.addInitialSchemaContext(schema);

        /*
         * DataBroker
         */
        rpcDataBroker = new JsonRPCDataBroker(peer, schema, pathMap, transportFactory, governance, jsonConverter);
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
        rpcBridge = new JsonRPCtoRPCBridge(peer, schema, pathMap, governance, transportFactory,
                jsonConverter);
        mountBuilder.addService(DOMRpcService.class, rpcBridge);
        pathMap.toMap(DataType.RPC).entrySet().stream().forEach(e -> newPeer.addRpcEndpoint(new RpcEndpointsBuilder()
                .setPath(e.getKey().getAsJsonObject().toString()).setEndpointUri(new Uri(e.getValue())).build()));

        /*
         * Notification service
         */
        notificationService = new JsonRPCNotificationService(peer, schema, pathMap, jsonConverter, transportFactory,
                governance);
        pathMap.toMap(DataType.NOTIFICATION).entrySet().stream()
                .forEach(e -> newPeer.addNotificationEndpoint(
                        new NotificationEndpointsBuilder().setPath(e.getKey().getAsJsonObject().toString())
                                .setEndpointUri(new Uri(e.getValue())).build()));
        mountBuilder.addService(DOMNotificationService.class, notificationService);

        mountpointRegistration = mountBuilder.register();

        // Publish operational state, list of modules contains all modules from effective schema
        final ActualEndpoints endpoint = new ActualEndpointsBuilder(newPeer).setModules(
                schema.getModules().stream().map(Module::getName).map(YangIdentifier::new).collect(Collectors.toList()))
                .build();
        final InstanceIdentifier<ActualEndpoints> peerOpId = InstanceIdentifier.builder(Config.class)
                .child(ActualEndpoints.class, new ActualEndpointsKey(newPeer.getName())).build();
        final WriteTransaction wrTrx = dataBroker.newWriteOnlyTransaction();
        wrTrx.put(LogicalDatastoreType.OPERATIONAL, peerOpId, endpoint);
        commitTransaction(wrTrx, peer.getName(), "Publish operational state");
    }

    static boolean supportInbandModels(Peer peer) {
        return (peer.getModules() != null && peer.getModules().size() == 1
                && peer.getModules().get(0).getValue().startsWith("jsonrpc-inband-models"));
    }

    @Override
    public void close() throws Exception {
        Stream.of(rpcDataBroker, rpcBridge, notificationService, mountpointRegistration).forEach(
            c -> Util.closeNullableWithExceptionCallback(c, e -> LOG.error("Failed to close provider {}", c, e)));
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

    public String getName() {
        return peer.getName();
    }

    @Override
    public String toString() {
        return "MappedPeerContext [peer=" + peer.getName() + ", path = " + biPath + "]";
    }
}
