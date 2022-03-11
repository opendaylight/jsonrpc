/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.single;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.jsonrpc.model.CombinedSchemaContextProvider;
import org.opendaylight.jsonrpc.model.GovernanceProvider;
import org.opendaylight.jsonrpc.provider.common.AbstractPeerContext;
import org.opendaylight.jsonrpc.provider.common.FailedPeerContext;
import org.opendaylight.jsonrpc.provider.common.MappedPeerContext;
import org.opendaylight.jsonrpc.provider.common.ProviderDependencies;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ForceRefreshInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ForceRefreshOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ForceRefreshOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ForceReloadInput;
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
    private static final DataTreeIdentifier<Config> CFG_DTI = DataTreeIdentifier
            .create(LogicalDatastoreType.CONFIGURATION, GLOBAL_CFG_II);
    private final Map<String, AbstractPeerContext> peerState = new ConcurrentHashMap<>();
    private final List<AutoCloseable> toClose = new LinkedList<>();
    private final ReentrantReadWriteLock changeLock = new ReentrantReadWriteLock();
    private volatile boolean closed = false;
    private final ProviderDependencies dependencies;
    private final GovernanceProvider governance;

    public JsonRPCProvider(ProviderDependencies dependencies, GovernanceProvider governance) {
        this.dependencies = Objects.requireNonNull(dependencies);
        this.governance = Objects.requireNonNull(governance);
    }

    /**
     * Get current configuration or operational state. Configuration can be set or deleted by the user via restconf.
     * Operational state will reflect actual result of configuration via restconf and other sources.
     */
    private Optional<Config> getConfig(LogicalDatastoreType store) {
        try (ReadTransaction roTrx = dependencies.getDataBroker().newReadOnlyTransaction()) {
            return Futures.getUnchecked(roTrx.read(store, GLOBAL_CFG_II));
        }
    }

    private static Map<String, Peer> generateCache(Collection<? extends Peer> arg) {
        return Optional.ofNullable(arg)
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(Peer::getName, Function.identity()));
    }

    private boolean processNotificationInternal() {
        LOG.debug("Processing notification");
        if (closed) {
            LOG.debug("{} was closed already, ignoring configuration change", ME);
            return false;
        }

        final Optional<Config> peersConfState = getConfig(LogicalDatastoreType.CONFIGURATION);
        if (!peersConfState.isPresent()) {
            // in case entire config was wiped, we still need to unconfigure
            // existing peers, hence supply empty list
            unmountPeers(new ConfigBuilder().setConfiguredEndpoints(Map.of()).build());
            LOG.info("{} configuration absent", ME);
            return false;
        }
        boolean result = mountPeers(peersConfState.get());
        result &= unmountPeers(peersConfState.get());
        return result;
    }

    private boolean unmountPeers(final Config peersConfState) {
        boolean result = true;
        final Map<String, Peer> cache = generateCache(peersConfState.nonnullConfiguredEndpoints().values());
        final List<String> toUnmountList = peerState.entrySet()
                .stream()
                .filter(e -> !cache.containsKey(e.getKey()))
                .map(Entry::getKey)
                .collect(Collectors.toList());
        for (final String toUnmount : toUnmountList) {
            result &= doUnmount(toUnmount);
        }
        return result;
    }

    private boolean mountPeers(final Config peersConfState) {
        boolean result = true;
        if (peersConfState.getConfiguredEndpoints() != null) {
            for (final Peer confPeer : peersConfState.nonnullConfiguredEndpoints().values()) {
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

    /**
     * Performs reconciliation between our internal mount state and the datastore upon startup and after receiving a
     * DCN. This method ensures serialized access to underlying datastructures via lock.
     */
    @GuardedBy("changeLock")
    private boolean processNotification() {
        final WriteLock wLock = changeLock.writeLock();
        try {
            wLock.lock();
            return processNotificationInternal();
        } finally {
            wLock.unlock();
        }
    }

    public void init() {
        toClose.add(dependencies.getDataBroker()
                .registerDataTreeChangeListener(CFG_DTI,
                        (ClusteredDataTreeChangeListener<Config>) changes -> processNotification()));
        processNotification();
    }

    @Override
    public void close() {
        closed = true;
        peerState.values().forEach(AbstractPeerContext::close);
        peerState.clear();
        toClose.forEach(Util::closeAndLogOnError);
    }

    /*
     * Do the heavy lifting required in order to mount.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    private boolean doMountDevice(Peer peer) {
        try {
            LOG.debug("Creating mapping context for peer {}", peer.getName());
            final CombinedSchemaContextProvider schemaProvider = new CombinedSchemaContextProvider(governance,
                    dependencies);
            final MappedPeerContext ctx = new MappedPeerContext(peer, dependencies.getTransportFactory(),
                    dependencies.getSchemaService(), dependencies.getDataBroker(),
                    dependencies.getDomMountPointService(), governance.get().orElse(null), schemaProvider);
            peerState.put(peer.getName(), ctx);
            LOG.info("Peer mounted : {}", ctx);
            return true;
        } catch (RuntimeException | URISyntaxException e) {
            LOG.error("Mount failed for peer '{}'", peer.getName(), e);
            peerState.put(peer.getName(), new FailedPeerContext(peer, dependencies.getDataBroker(), e));
            return false;
        }
    }

    /*
     * unMount heavy lifter - does the appropriate cleanup when unmounting a device
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public boolean doUnmount(String deviceName) {
        if (Strings.isNullOrEmpty(deviceName)) {
            return false;
        }
        final AbstractPeerContext toDelete = peerState.remove(deviceName);
        if (toDelete == null) {
            LOG.error("Device '{}' did not complete mount, cannot remove", deviceName);
            return false;
        } else {
            try {
                LOG.debug("Destroying mapping context of peer '{}'", toDelete);
                toDelete.close();
                LOG.debug("Device '{}' unmounted successfully", deviceName);
                return true;
            } catch (Exception e) {
                LOG.error("Device '{}' unmount, failed", deviceName, e);
                return false;
            }
        }
    }

    /*
     * JSON Rpc Force refresh
     */
    @Override
    public ListenableFuture<RpcResult<ForceRefreshOutput>> forceRefresh(ForceRefreshInput input) {
        LOG.debug("Refreshing json rpc state");
        return Futures.immediateFuture(RpcResultBuilder
                .<ForceRefreshOutput>success(new ForceRefreshOutputBuilder().setResult(processNotification()).build())
                .build());
    }

    /*
     * JSON Rpc Force reload
     */
    @Override
    public ListenableFuture<RpcResult<ForceReloadOutput>> forceReload(ForceReloadInput input) {
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
}
