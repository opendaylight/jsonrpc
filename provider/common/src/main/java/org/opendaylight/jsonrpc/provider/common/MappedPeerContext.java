/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import com.google.gson.JsonElement;
import java.net.URISyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumHashMap;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.hmap.JsonPathCodec;
import org.opendaylight.jsonrpc.impl.JsonConverter;
import org.opendaylight.jsonrpc.impl.JsonRPCDataBroker;
import org.opendaylight.jsonrpc.impl.JsonRPCNotificationService;
import org.opendaylight.jsonrpc.impl.JsonRPCtoRPCBridge;
import org.opendaylight.jsonrpc.model.MutablePeer;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.model.SchemaContextProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPoint;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.MountStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataConfigEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataOperationalEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.NotificationEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpointsBuilder;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;

/**
 * Context of mapped {@link Peer}.
 *
 * <p>
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public class MappedPeerContext extends AbstractPeerContext {
    private final HierarchicalEnumMap<JsonElement, DataType, String> pathMap = HierarchicalEnumHashMap
            .create(DataType.class, JsonPathCodec.create());
    private final JsonConverter jsonConverter;
    private final JsonRPCtoRPCBridge rpcBridge;
    private final JsonRPCDataBroker rpcDataBroker;
    private final JsonRPCNotificationService notificationService;
    private final ObjectRegistration<DOMMountPoint> mountpointRegistration;
    private final YangInstanceIdentifier biPath;

    @SuppressWarnings("checkstyle:IllegalCatch")
    public MappedPeerContext(@NonNull Peer peer, @NonNull TransportFactory transportFactory,
            @NonNull DOMSchemaService schemaService, @NonNull DataBroker dataBroker,
            @NonNull DOMMountPointService mountService, @Nullable RemoteGovernance governance,
            @NonNull SchemaContextProvider schemaProvider) throws URISyntaxException {
        super(peer, dataBroker);

        publishState(new ActualEndpointsBuilder(peer), MountStatus.Initial);
        final MutablePeer newPeer = new MutablePeer().name(peer.getName());
        publishState(new ActualEndpointsBuilder(peer), MountStatus.Processing);
        final EffectiveModelContext schema = schemaProvider.createSchemaContext(peer);
        // actual list of modules lies in created SchemaContext
        newPeer.addModels(
                schema.getModules().stream().map(m -> new YangIdentifier(m.getName())).collect(Collectors.toList()));

        biPath = Util.createBiPath(peer.getName());

        final DOMMountPointService.DOMMountPointBuilder mountBuilder = mountService.createMountPoint(biPath);
        jsonConverter = new JsonConverter(schema);
        mountBuilder.addInitialSchemaContext(schema);

        /*
         * DataBroker
         */
        rpcDataBroker = new JsonRPCDataBroker(peer, schema, pathMap, transportFactory, governance, jsonConverter);
        mountBuilder.addService(DOMDataBroker.class, rpcDataBroker);

        pathMap.toMap(DataType.CONFIGURATION_DATA)
                .entrySet()
                .stream()
                .forEach(e -> newPeer.addDataConfigEndpoint(
                        new DataConfigEndpointsBuilder().setPath(e.getKey().getAsJsonObject().toString())
                                .setEndpointUri(new Uri(e.getValue()))
                                .build()));
        pathMap.toMap(DataType.OPERATIONAL_DATA)
                .entrySet()
                .stream()
                .forEach(e -> newPeer.addDataOperationalEndpoint(
                        new DataOperationalEndpointsBuilder().setPath(e.getKey().getAsJsonObject().toString())
                                .setEndpointUri(new Uri(e.getValue()))
                                .build()));

        /*
         * RPC bridge
         */
        rpcBridge = new JsonRPCtoRPCBridge(peer, schema, pathMap, governance, transportFactory, jsonConverter);
        mountBuilder.addService(DOMRpcService.class, rpcBridge);
        pathMap.toMap(DataType.RPC)
                .entrySet()
                .stream()
                .forEach(e -> newPeer
                        .addRpcEndpoint(new RpcEndpointsBuilder().setPath(e.getKey().getAsJsonObject().toString())
                                .setEndpointUri(new Uri(e.getValue()))
                                .build()));

        /*
         * Notification service
         */
        notificationService = new JsonRPCNotificationService(peer, schema, pathMap, jsonConverter, transportFactory,
                governance);
        pathMap.toMap(DataType.NOTIFICATION)
                .entrySet()
                .stream()
                .forEach(e -> newPeer.addNotificationEndpoint(
                        new NotificationEndpointsBuilder().setPath(e.getKey().getAsJsonObject().toString())
                                .setEndpointUri(new Uri(e.getValue()))
                                .build()));
        mountBuilder.addService(DOMNotificationService.class, notificationService);

        mountpointRegistration = mountBuilder.register();

        // Publish operational state, list of modules contains all modules from effective schema
        final ActualEndpointsBuilder endpoint = new ActualEndpointsBuilder(newPeer).setModules(schema.getModules()
                .stream()
                .map(Module::getName)
                .map(YangIdentifier::new)
                .collect(Collectors.toList()));
        publishState(endpoint, MountStatus.Mounted);
    }

    @Override
    public void close() {
        Stream.of(rpcDataBroker, rpcBridge, notificationService, mountpointRegistration)
                .forEach(Util::closeAndLogOnError);
        super.close();
    }

    public String getName() {
        return peer.getName();
    }

    @Override
    public String toString() {
        return "MappedPeerContext [peer=" + peer.getName() + ", path = " + biPath + "]";
    }
}
