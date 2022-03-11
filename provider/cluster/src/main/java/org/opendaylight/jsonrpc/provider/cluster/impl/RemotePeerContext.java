/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.impl;

import static akka.pattern.Patterns.ask;
import static org.opendaylight.jsonrpc.provider.cluster.impl.ClusterUtil.createMasterActorName;

import akka.actor.ActorRef;
import akka.cluster.Cluster;
import akka.dispatch.OnComplete;
import akka.util.Timeout;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.JsonElement;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumHashMap;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.hmap.JsonPathCodec;
import org.opendaylight.jsonrpc.impl.JsonRPCDataBroker;
import org.opendaylight.jsonrpc.impl.JsonRPCtoRPCBridge;
import org.opendaylight.jsonrpc.impl.JsonRpcDOMSchemaService;
import org.opendaylight.jsonrpc.model.CombinedSchemaContextProvider;
import org.opendaylight.jsonrpc.model.MutablePeer;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.provider.cluster.messages.InitMasterMountPoint;
import org.opendaylight.jsonrpc.provider.common.AbstractPeerContext;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPoint;
import org.opendaylight.mdsal.dom.api.DOMMountPointService.DOMMountPointBuilder;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.MountStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataConfigEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataOperationalEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpointsBuilder;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RemotePeerContext extends AbstractPeerContext implements ClusterSingletonService {
    private static final Logger LOG = LoggerFactory.getLogger(RemotePeerContext.class);
    private final ServiceGroupIdentifier sgi;
    private final String name;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final ClusterDependencies dependencies;
    private ActorRef masterActorRef;
    private final Peer peer;
    private SlavePeerContext slaveContext;
    private ObjectRegistration<DOMMountPoint> mountPointReg;
    private final String selfAddress;

    RemotePeerContext(Peer peer, ClusterDependencies dependencies) {
        super(peer, dependencies.getDataBroker());
        name = peer.getName();
        this.peer = peer;
        sgi = ServiceGroupIdentifier.create(Util.createBiPath(peer.getName()).toString());
        this.dependencies = dependencies;
        slaveContext = new SlavePeerContext(peer, dependencies);
        selfAddress = Cluster.get(dependencies.getActorSystem()).selfAddress().toString();
    }

    @Override
    public void close() {
        LOG.debug("Closing {}", this);
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        stopSlave();
        stopMaster();
    }

    private void stopSlave() {
        if (slaveContext != null) {
            slaveContext.close();
            slaveContext = null;
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public void instantiateServiceInstance() {
        if (!closed.get()) {
            LOG.info("[{}] Ownership granted to {}", name, selfAddress);
            stopSlave();
            masterActorRef = dependencies.getActorSystem()
                    .actorOf(RemotePeerActor.props(peer, dependencies), createMasterActorName(name, selfAddress));
            try {
                startMaster();
            } catch (Exception e) {
                LOG.error("Unable to create mountpoint", e);
                publishState(new ActualEndpointsBuilder(peer), MountStatus.Failed, Optional.of(e));
            }
        }
    }

    @Override
    public ListenableFuture<? extends Object> closeServiceInstance() {
        LOG.info("[{}] Ownership withdrawn from {}", name, selfAddress);
        if (!closed.get()) {
            slaveContext = new SlavePeerContext(peer, dependencies);
            stopMaster();
        }
        return FluentFutures.immediateNullFluentFuture();
    }

    @Override
    public @NonNull ServiceGroupIdentifier getIdentifier() {
        return sgi;
    }

    private void stopMaster() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }
        Util.closeAndLogOnError(mountPointReg);
        if (masterActorRef != null) {
            LOG.info("[{}] Stopping {}", name, masterActorRef);
            dependencies.getActorSystem().stop(masterActorRef);
            masterActorRef = null;
            removeOperationalState();
        }
    }

    private void startMaster() {
        waitForMountpoint();
        final CombinedSchemaContextProvider schemaFactory = new CombinedSchemaContextProvider(
                dependencies.getGovernanceProvider(), dependencies);

        final DOMMountPointBuilder builder = dependencies.getDomMountPointService()
                .createMountPoint(Util.createBiPath(peer.getName()));
        final EffectiveModelContext schema = schemaFactory.createSchemaContext(peer);

        final RemoteGovernance governance = dependencies.getGovernanceProvider().get().orElse(null);

        final MutablePeer newPeer = new MutablePeer().name(peer.getName());
        final HierarchicalEnumMap<JsonElement, DataType, String> pathMap = HierarchicalEnumHashMap
                .create(DataType.class, JsonPathCodec.create());
        populatePathMap(pathMap, peer);

        final JsonRpcCodecFactory codecFactory = new JsonRpcCodecFactory(schema);

        final JsonRPCDataBroker rpcDataBroker = new JsonRPCDataBroker(peer, schema, pathMap,
                dependencies.getTransportFactory(), governance, codecFactory);
        builder.addService(DOMDataBroker.class, rpcDataBroker);

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

        JsonRPCtoRPCBridge rpcBridge = new JsonRPCtoRPCBridge(peer, schema, pathMap, governance,
                dependencies.getTransportFactory(), codecFactory);
        builder.addService(DOMRpcService.class, rpcBridge);
        pathMap.toMap(DataType.RPC)
                .entrySet()
                .stream()
                .forEach(e -> newPeer
                        .addRpcEndpoint(new RpcEndpointsBuilder().setPath(e.getKey().getAsJsonObject().toString())
                                .setEndpointUri(new Uri(e.getValue()))
                                .build()));
        JsonRpcDOMSchemaService peerSchemaService = new JsonRpcDOMSchemaService(newPeer, schema);
        builder.addService(DOMSchemaService.class, peerSchemaService);
        mountPointReg = builder.register();
        ask(masterActorRef, new InitMasterMountPoint(rpcDataBroker, rpcBridge), Timeout.apply(10, TimeUnit.SECONDS))
                .onComplete(new OnComplete<>() {
                    @Override
                    public void onComplete(final Throwable failure, final Object success) {
                        if (failure == null) {
                            publishState(
                                    new ActualEndpointsBuilder(peer).setModules(schema.getModules()
                                            .stream()
                                            .map(m -> new YangIdentifier(m.getName()))
                                            .collect(Collectors.toSet())),
                                    MountStatus.Mounted, Optional.empty(), selfAddress);
                        } else {
                            publishState(new ActualEndpointsBuilder(peer), MountStatus.Failed, Optional.of(failure),
                                    selfAddress);
                        }
                    }
                }, dependencies.getActorSystem().dispatcher());
    }

    // when ownership is granted to this node, it is possible that slave mountpoint still exists
    // so wait for it to disappear here
    private void waitForMountpoint() {
        int tries = 10;
        while (--tries > 0) {
            if (!dependencies.getDomMountPointService().getMountPoint(Util.createBiPath(peer.getName())).isPresent()) {
                return;
            } else {
                LOG.debug("[{}] Mounpoint still exists, waiting for a while", peer.getName());
                Uninterruptibles.sleepUninterruptibly(250, TimeUnit.MILLISECONDS);
            }
        }
        throw new IllegalStateException("Mountpoint still exists : " + peer.getName());
    }

    @Override
    public String toString() {
        return "RemotePeerContext [name=" + name + ", closed=" + closed + ", stopped=" + stopped + "]";
    }
}
