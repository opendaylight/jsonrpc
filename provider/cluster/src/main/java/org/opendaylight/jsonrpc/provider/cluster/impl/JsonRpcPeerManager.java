/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.impl;

import static org.opendaylight.jsonrpc.provider.cluster.impl.ClusterUtil.getPeerOpstateIdentifier;

import akka.actor.ActorRef;
import akka.cluster.Cluster;
import akka.util.Timeout;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumHashMap;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.hmap.JsonPathCodec;
import org.opendaylight.jsonrpc.impl.JsonConverter;
import org.opendaylight.jsonrpc.impl.JsonRPCDataBroker;
import org.opendaylight.jsonrpc.impl.JsonRPCtoRPCBridge;
import org.opendaylight.jsonrpc.model.CombinedSchemaContextProvider;
import org.opendaylight.jsonrpc.provider.cluster.api.JsonRpcPeerSingletonService;
import org.opendaylight.jsonrpc.provider.common.AbstractPeerContext;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPoint;
import org.opendaylight.mdsal.dom.api.DOMMountPointService.DOMMountPointBuilder;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.MountStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpointsBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager of {@link Peer}'s state in clustered environment. Takes care of creating mountpoint on slave node based on
 * change in {@link Peer}'s operational status.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 1, 2020
 */
public class JsonRpcPeerManager extends AbstractPeerContext implements ClusteredDataTreeChangeListener<ActualEndpoints>,
        JsonRpcPeerSingletonService, AutoCloseable, ClusterSingletonService {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcPeerManager.class);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private ListenerRegistration<JsonRpcPeerManager> dtclRegistration;
    private final Peer peer;
    private final ClusterDependencies dependencies;
    private final Timeout askTimeout;
    private final AtomicBoolean master = new AtomicBoolean(false);
    private final ServiceGroupIdentifier sgi;
    private final CombinedSchemaContextProvider schemaFactory;
    private final ProxiedDOMService<DOMRpcService> rpcService;
    private final ProxiedDOMService<DOMDataBroker> dataBroker;
    private Optional<Throwable> mountFailure = Optional.empty();
    private ActorRef actorRef;
    private ObjectRegistration<DOMMountPoint> mountPointReg;
    private final ClusterSingletonServiceRegistration clusterRegistration;

    public JsonRpcPeerManager(@NonNull ClusterDependencies dependencies, @NonNull Peer peer,
            @NonNull Timeout askTimeout) {
        super(peer, Objects.requireNonNull(dependencies).getDataBroker());
        this.dependencies = dependencies;
        this.peer = Objects.requireNonNull(peer);
        this.askTimeout = Objects.requireNonNull(askTimeout);
        sgi = ServiceGroupIdentifier.create("jsonrpc/peer/" + peer.getName());
        schemaFactory = new CombinedSchemaContextProvider(dependencies.getGovernanceProvider(), dependencies);
        rpcService = new ProxiedDOMService<>(DOMRpcService.class);
        dataBroker = new ProxiedDOMService<>(DOMDataBroker.class);
        startOpStateListener();
        recreateInitialContext();
        clusterRegistration = dependencies.getClusterSingletonServiceProvider().registerClusterSingletonService(this);
    }

    private boolean recreateInitialContext() {
        try {
            final DOMMountPointBuilder builder = dependencies.getDomMountPointService()
                    .createMountPoint(Util.createBiPath(peer.getName()));
            builder.addInitialSchemaContext(schemaFactory.createSchemaContext(peer));
            builder.addService(DOMDataBroker.class, dataBroker.getProxy());
            builder.addService(DOMRpcService.class, rpcService.getProxy());
            mountPointReg = builder.register();
            mountFailure = Optional.empty();
        } catch (RuntimeException e) {
            LOG.error("Unable to create initial mount point context", e);
            mountFailure = Optional.of(e);
        }
        return !mountFailure.isPresent();
    }

    @Override
    public void onDataTreeChanged(@NonNull Collection<DataTreeModification<ActualEndpoints>> changes) {
        for (final DataTreeModification<ActualEndpoints> change : changes) {
            final DataObjectModification<ActualEndpoints> rootNode = change.getRootNode();
            LOG.debug("[{}]: OP DTC {} => {}", peer.getName(), rootNode.getDataBefore(), rootNode.getDataAfter());
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                    peerOpStateChanged(rootNode.getDataAfter());
                    break;
                case WRITE:
                    peerOpStateChanged(rootNode.getDataAfter());
                    break;
                case DELETE:
                    disableMountpoint(true);
                    break;
                default:
                    LOG.debug("[{}]: Unhandled modification: {}", peer.getName(), rootNode.getModificationType());
            }
        }
    }

    public void reset(Peer peer) {
        // TODO
        LOG.debug("[{}] : Peer reset is not (yet) implemented", peer.getName());
    }

    public void setupMaster() {
        stopOpStateListener();
        if (mountFailure.isPresent()) {
            if (!recreateInitialContext()) {
                publishState(new ActualEndpointsBuilder(peer), MountStatus.Failed, mountFailure);
                return;
            }
        }

        final EffectiveModelContext schema = mountPointReg.getInstance().getEffectiveModelContext();
        final JsonConverter jsonConverter = new JsonConverter(schema);
        final HierarchicalEnumMap<JsonElement, DataType, String> pathMap = HierarchicalEnumHashMap
                .create(DataType.class, JsonPathCodec.create());
        dataBroker.reset(new JsonRPCDataBroker(peer, schema, pathMap, dependencies.getTransportFactory(),
                dependencies.getGovernanceProvider().get().orElse(null), jsonConverter));

        try {
            rpcService.reset(new JsonRPCtoRPCBridge(peer, schema, pathMap,
                    dependencies.getGovernanceProvider().get().orElse(null), dependencies.getTransportFactory(),
                    jsonConverter));
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        disableMountpoint(false);
        final String masterAddress = Cluster.get(dependencies.getActorSystem()).selfAddress().toString();
        publishState(new ActualEndpointsBuilder(peer), MountStatus.Mounted, Optional.empty(), masterAddress);
    }

    @Override
    public @NonNull ServiceGroupIdentifier getIdentifier() {
        return sgi;
    }

    @Override
    public void instantiateServiceInstance() {
        if (!closed.get()) {
            LOG.info("[{}] Cluster singleton starting", peer.getName());
            master.set(true);
            setupMaster();
        }
    }

    @Override
    public ListenableFuture<? extends Object> closeServiceInstance() {
        if (!closed.get()) {
            LOG.info("[{}] Cluster singleton stopping", peer.getName());
            master.set(false);
            setupSlave();
        }
        return FluentFutures.immediateNullFluentFuture();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            LOG.info("[{}] Closing", peer.getName());
            disableMountpoint(true);
            stopOpStateListener();
            clusterRegistration.close();
            mountPointReg.close();
        }
    }

    public void setupSlave() {
        dataBroker.reset(new SlaveDOMDataBroker(peer));
        rpcService.reset(new SlaveDOMRpcService(dependencies.getActorSystem(), askTimeout, actorRef, peer.getName()));
    }

    private void startOpStateListener() {
        dtclRegistration = dependencies.getDataBroker()
                .registerDataTreeChangeListener(getPeerOpstateIdentifier(peer.getName()), this);
    }

    private void disableMountpoint(boolean disable) {
        rpcService.disable(disable);
        dataBroker.disable(disable);
    }

    private void peerOpStateChanged(@Nullable ActualEndpoints node) {
        if (closed.get()) {
            LOG.info("[{}] Ignoring state change c={},s={}", peer.getName(), closed.get(), node.getMountStatus());
            return;
        }
        if (MountStatus.Mounted.equals(node.getMountStatus())) {
            LOG.info("[{}] Configuring for slave mode", peer.getName());
            resetActor(node.getManagedBy());
            setupSlave();
        } else {
            disableMountpoint(true);
        }
    }

    private void stopOpStateListener() {
        if (dtclRegistration != null) {
            Util.closeAndLogOnError(dtclRegistration);
            dtclRegistration = null;
        }
    }

    private void resetActor(String address) {
        actorRef = dependencies.getActorSystem().actorOf(RemotePeerActor.props(peer, askTimeout, dependencies));
    }

    @Override
    public String toString() {
        return "JsonRpcPeerManager [closed=" + closed + ", peer=" + peer + "]";
    }
}
