/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.provider.common.RpcClient;
import org.opendaylight.mdsal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.mdsal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public final class JsonRPCtoRPCBridge extends AbstractJsonRPCComponent implements DOMRpcService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCtoRPCBridge.class);
    private final Map<QName, RpcClient> mappedRpcs;
    private final Collection<DOMRpcIdentifier> availableRpcs;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Instantiates a new RPC Bridge.
     *
     * @param peer the peer name
     * @param schemaContext the schema context
     * @param pathMap endpoint mapping
     * @param governance additional json rpc service to query for endpoints
     * @param transportFactory - JSON RPC 2.0 transport factory
     */
    public JsonRPCtoRPCBridge(@NonNull Peer peer, @NonNull EffectiveModelContext schemaContext,
            @NonNull HierarchicalEnumMap<JsonElement, DataType, String> pathMap, @Nullable RemoteGovernance governance,
            @NonNull TransportFactory transportFactory, @NonNull JsonRpcCodecFactory codecFactory) {
        super(schemaContext, transportFactory, pathMap, codecFactory, peer);
        final ImmutableMap.Builder<QName, RpcClient> mappedRpcsBuilder = ImmutableMap.builder();
        for (final RpcDefinition def : schemaContext.getOperations()) {
            addRpcDefinition(governance, def, mappedRpcsBuilder);
        }

        mappedRpcs = mappedRpcsBuilder.build();
        availableRpcs = mappedRpcs.keySet().stream().map(DOMRpcIdentifier::create).collect(Collectors.toSet());

        if (mappedRpcs.isEmpty()) {
            LOG.info("[{}] No RPCs mapped", peer.getName());
        } else {
            LOG.info("[{}] RPC bridge instantiated with {} methods", peer.getName(), mappedRpcs.size());
        }
    }

    private void addRpcDefinition(RemoteGovernance governance, RpcDefinition def,
            ImmutableMap.Builder<QName, RpcClient> mapped) {
        final QNameModule qm = def.getQName().getModule();
        final String localName = def.getQName().getLocalName();
        final Optional<Module> possibleModule = schemaContext.findModule(qm.getNamespace(), qm.getRevision());
        final JsonObject path = createRootPath(possibleModule.get(), def.getQName());
        final String endpoint = getEndpoint(DataType.RPC, governance, path);
        if (endpoint != null) {
            LOG.info("[{}] RPC '{}' mapped to {}", localName, endpoint, peer.getName());
            mapped.put(def.getQName(), new RpcClient(codecFactory, def, transportFactory, endpoint));
        } else {
            LOG.warn("[{}] RPC '{}' cannot be mapped, no known endpoint", localName, peer.getName());
        }
    }

    @Override
    public ListenableFuture<DOMRpcResult> invokeRpc(@NonNull final QName type,
            @Nullable final NormalizedNode<?, ?> input) {
        if (closed.get()) {
            return bridgeNotAvailable();
        }

        if (mappedRpcs.containsKey(type)) {
            return mappedRpcs.get(type).invoke(input);
        } else {
            return FluentFutures.immediateFailedFluentFuture(
                    new DOMRpcImplementationNotAvailableException("No endpoint is mapped to RPC %s", type));
        }

    }

    private FluentFuture<DOMRpcResult> bridgeNotAvailable() {
        return FluentFutures
                .immediateFailedFluentFuture(new DOMRpcImplementationNotAvailableException("RPC Bridge shutting down"));
    }

    @Override
    public <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(
            @NonNull final T listener) {
        LOG.info("registered RPC implementation for json rpc broker");
        listener.onRpcAvailable(availableRpcs);
        return new AbstractListenerRegistration<>(listener) {
            @Override
            protected void removeRegistration() {
                // NOOP, no rpcs appear and disappear in this implementation
            }
        };
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            mappedRpcs.values().stream().forEach(RpcClient::close);
        }
    }
}
