/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.stream.Collectors;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.jsonrpc.bus.messagelib.EndpointRole;
import org.opendaylight.jsonrpc.bus.messagelib.ThreadedSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.model.SchemaContextProvider;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ForceRefreshOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ForceRefreshOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ForceReloadOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ForceReloadOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.JsonrpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRPCProvider implements JsonrpcService, AutoCloseable {
    private static final String ME = "JSON RPC Provider";
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCProvider.class);
    private static final InstanceIdentifier<Config> GLOBAL_CFG_II = InstanceIdentifier.create(Config.class);
    private static final DataTreeIdentifier<Config> OPER_DTI = new DataTreeIdentifier<>(
            LogicalDatastoreType.OPERATIONAL, GLOBAL_CFG_II);
    private static final DataTreeIdentifier<Config> CFG_DTI = new DataTreeIdentifier<>(
            LogicalDatastoreType.CONFIGURATION, GLOBAL_CFG_II);
    private TransportFactory transportFactory;
    private DataBroker dataBroker;
    private DOMDataBroker domDataBroker;
    private DOMSchemaService schemaService;
    private RemoteGovernance governance;
    private ThreadedSession remoteControl = null;
    private final Map<String, MappedPeerContext> peerState = Maps.newConcurrentMap();
    private final List<AutoCloseable> toClose = new LinkedList<>();
    private final ReentrantReadWriteLock changeLock = new ReentrantReadWriteLock();
    private volatile boolean sessionInitialized = false;
    private volatile boolean providerClosed = false;
    private DOMMountPointService domMountPointService;
    private BindingToNormalizedNodeCodec codec;

    /**
     * Get current configuration state configuration can be set or deleted by
     * the user via restconf. Operational state will reflect actual result of
     * configuration via restconf and other sources.
     */
    private Config getConfig() {
        final ReadOnlyTransaction roTrx = dataBroker.newReadOnlyTransaction();
        try {
            return roTrx.read(LogicalDatastoreType.CONFIGURATION, GLOBAL_CFG_II).checkedGet().orNull();
        } catch (ReadFailedException e) {
            LOG.error("Failed to read configuration", e);
            return null;
        } finally {
            roTrx.close();
        }
    }

    private Map<String, Peer> generateCache(List<? extends Peer> arg) {
        if (arg != null) {
            final Map<String, Peer> result = new HashMap<>();
            for (Peer peer : arg) {
                result.put(peer.getName(), peer);
            }
            return result;
        }
        return Collections.emptyMap();
    }

    /**
     * Gets the root Om from the datastore.
     */
    private Uri rootOm() {
        final Config result = getConfig();
        Preconditions.checkNotNull(result, "Configuration not present!");
        return result.getGovernanceRoot();
    }

    private boolean processNotificationInternal() throws URISyntaxException {
        LOG.debug("Processing notification");
        if (!sessionInitialized) {
            LOG.debug("Can't process configuration change at this time, need provider session first");
            return false;
        }

        if (providerClosed) {
            LOG.debug("{} was closed already, ignoring configuration change", ME);
            return false;
        }

        final Config peersConfState = getConfig();
        if (peersConfState == null) {
            LOG.info("{} configuration absent", ME);
            return false;
        }

        /*
         * 1. Governance There is little we can do if governance is not set as
         * we cannot fetch models which come from the same root om
         *
         */
        if (!resetGovernance(peersConfState)) {
            return false;
        }

        /*
         * 2. Remote control ORB We open the remote control interface upon its
         * URI being set in the config datastore. Remote control will offer the
         * same interface as the peer settings available via restconf but to
         * operational datastore.
         */
        if (!initRemoteControl(peersConfState)) {
            return false;
        }

        /*
         * 3. Handle insertions and changes We walk both the config list and
         * perform a doMount().
         *
         */
        boolean result = mountPeers(peersConfState);

        /* 4, Unmount peers */
        result &= unmountPeers(peersConfState);
        return result;
    }

    private boolean unmountPeers(final Config peersConfState) {
        boolean result = true;
        final Map<String, Peer> cache = generateCache(peersConfState.getConfiguredEndpoints());
        final List<String> toUnmountList = peerState.entrySet().stream().filter(e -> !cache.containsKey(e.getKey()))
                .map(Entry::getKey).collect(Collectors.toList());
        for (final String toUnmount : toUnmountList) {
            result &= doUnmount(toUnmount);
        }
        return result;
    }

    private boolean mountPeers(final Config peersConfState) throws URISyntaxException {
        boolean result = true;
        if (peersConfState.getConfiguredEndpoints() != null) {
            for (final Peer confPeer : peersConfState.getConfiguredEndpoints()) {
                LOG.debug("Processing peer from conf {}", confPeer.getName());
                if (!peerState.containsKey(confPeer.getName())) {
                    result &= doMountDevice(confPeer);
                }
            }
        } else {
            LOG.debug("No configured endpoints");
        }
        return result;
    }

    private boolean initRemoteControl(final Config peersConfState) {
        if (remoteControl == null && peersConfState.getWhoAmI() != null) {
            /* remote control ORB not initialized */
            LOG.debug("Initializing remote control to {}", peersConfState.getWhoAmI());
            try {
                remoteControl = transportFactory.createResponder(peersConfState.getWhoAmI().getValue(),
                        new RemoteControl(domDataBroker, schemaService.getGlobalContext(), codec));
            } catch (URISyntaxException e) {
                LOG.error("Invalid URI provided, can't continue", e);
                return false;
            }
        }
        return true;
    }

    private boolean resetGovernance(Config peersConfState) {
        try {
            LOG.debug("(Re)setting governance root for JSON RPC to {}", peersConfState.getGovernanceRoot());
            if (governance != null) {
                Util.closeNullableWithExceptionCallback(governance,
                    t -> LOG.warn("Failed to close RemoteGovernance", t));
                governance = null;
            }
            final Uri rootOm = rootOm();
            if (rootOm != null) {
                // Need to re-create proxy, because root-om can point to URI
                // with different transport then before
                governance = transportFactory.createProxy(RemoteGovernance.class,
                        Util.ensureRole(rootOm().getValue(), EndpointRole.REQ));
            } else {
                governance = null;
            }
            return true;
        } catch (IllegalStateException e) {
            LOG.error("Governance root for JSON-RPC not set, cannot fetch models, refusing to continue", e);
            return false;
        } catch (URISyntaxException e) {
            LOG.error("Invalid URI provided, can't continue", e);
            return false;
        }
    }

    /**
     * Performs reconciliation between our internal mount state and the
     * datastore upon startup and after receiving a DCN.
     */
    @GuardedBy("changeLock")
    private boolean processNotification() {
        final WriteLock wLock = changeLock.writeLock();
        try {
            wLock.lock();
            return processNotificationInternal();
        } catch (URISyntaxException e) {
            LOG.error("Invalid URI was speecified in configuration", e);
            return false;
        } finally {
            wLock.unlock();
        }
    }

    public void init() {
        LOG.debug("JSON RPC Provider init");
        // fail-fast validation
        Objects.requireNonNull(transportFactory, "TransportFactory was not set");
        Objects.requireNonNull(dataBroker, "DataBroker was not set");
        Objects.requireNonNull(domDataBroker, "DOMDataBroker was not set");
        Objects.requireNonNull(domMountPointService, "DOMMountPointService was not set");
        Objects.requireNonNull(codec, "BindingToNromalizedNodeCodec was not set");
        toClose.add(codec);
        toClose.add(dataBroker.registerDataTreeChangeListener(OPER_DTI,
                (ClusteredDataTreeChangeListener<Config>) changes -> processNotification()));
        toClose.add(dataBroker.registerDataTreeChangeListener(CFG_DTI,
                (ClusteredDataTreeChangeListener<Config>) changes -> processNotification()));
        sessionInitialized = true;
        processNotification();
    }

    @Override
    public void close() throws Exception {
        peerState.values().forEach(p -> doUnmount(p.getName()));
        peerState.clear();
        if (remoteControl != null) {
            remoteControl.stop();
            remoteControl.joinAndClose();
        }
        Util.closeNullable(governance);
        governance = null;
        toClose.forEach(
            c -> Util.closeNullableWithExceptionCallback(c, e -> LOG.warn("Failed to close object {}", c, e)));
        org.opendaylight.jsonrpc.bus.messagelib.Util.close();
        LOG.debug("JsonRPCProvider Closed");
        providerClosed = true;
    }

    /**
     * Validates {@link Peer} before attempts to mount.
     *
     * @param peer {@link Peer} to validates
     * @return true if {@link Peer}'s configuration is valid, false otherwise
     */
    private boolean isPeerConfigValid(Peer peer) {
        if (governance == null) {
            // DataBrokerRequirements
            if (peer.getDataConfigEndpoints() == null) {
                return false;
            }
            if (peer.getDataOperationalEndpoints() == null) {
                return false;
            }
        }
        return true;
    }

    /*
     * Do the heavy lifting required in order to mount Presently - configures
     * only data
     */
    public boolean doMountDevice(Peer peer) throws URISyntaxException {
        if (!sessionInitialized) {
            return false;
        }
        if (!isPeerConfigValid(peer)) {
            LOG.error("Peer configuration is not valid, refusing to mount : '{}'", peer);
            return false;
        }
        LOG.debug("Creating mapping context for peer {}", peer.getName());
        final MappedPeerContext ctx = new MappedPeerContext(peer, transportFactory, getSchemaContextProvider(),
                dataBroker, domMountPointService, governance);
        peerState.put(peer.getName(), ctx);
        LOG.info("Peer mounted : {}", ctx);
        return true;
    }

    /*
     * unMount heavy lifter - does the appropriate cleanup when unmounting a
     * device
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public boolean doUnmount(String deviceName) {
        if (Strings.isNullOrEmpty(deviceName)) {
            return false;
        }
        final MappedPeerContext toDelete = peerState.remove(deviceName);
        if (toDelete == null) {
            LOG.error("Device '{}' did not complete mount, cannot remove", deviceName);
            return false;
        } else {
            try {
                LOG.debug("Destroying mapping context of peer '{}'", toDelete.getName());
                toDelete.close();
                LOG.debug("Device '{}' unmounted successfully", deviceName);
                return true;
            } catch (Exception e) {
                LOG.error("Device '{}'  unmount, raised {} ", deviceName, e);
                return false;
            }
        }
    }

    private SchemaContextProvider getSchemaContextProvider() {
        return governance != null ? new GovernanceSchemaContextProvider(governance)
                : new BuiltinSchemaContextProvider(schemaService.getGlobalContext());
    }

    /*
     * JSON Rpc Force refresh
     */
    @Override
    public Future<RpcResult<ForceRefreshOutput>> forceRefresh() {
        LOG.debug("Refreshing json rpc state");
        return Futures.immediateFuture(RpcResultBuilder
                .<ForceRefreshOutput>success(new ForceRefreshOutputBuilder().setResult(processNotification()).build())
                .build());
    }

    /*
     * JSON Rpc Force reload
     */
    @Override
    public Future<RpcResult<ForceReloadOutput>> forceReload() {
        final ForceReloadOutputBuilder outputBuilder = new ForceReloadOutputBuilder();
        LOG.debug("Remounting all json rpc peers");
        boolean result = true;
        final List<String> toUnmountList = new ArrayList<>();
        toUnmountList.addAll(peerState.keySet());
        for (final String toUnmount : toUnmountList) {
            result &= doUnmount(toUnmount);
        }
        result &= processNotification();
        outputBuilder.setResult(result);
        return Futures.immediateFuture(RpcResultBuilder.<ForceReloadOutput>success(outputBuilder.build()).build());
    }

    /*
     * Public setters
     */

    public void setTransportFactory(TransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void setCodec(BindingToNormalizedNodeCodec codec) {
        this.codec = codec;
    }

    public void setDomMountPointService(DOMMountPointService domMountPointService) {
        this.domMountPointService = domMountPointService;
    }

    public void setDomDataBroker(DOMDataBroker domDataBroker) {
        this.domDataBroker = domDataBroker;
    }

    public void setSchemaService(DOMSchemaService schemaService) {
        this.schemaService = schemaService;
    }
}
