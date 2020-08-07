/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.JsonReaderAdapter;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.model.RpcState;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.opendaylight.mdsal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JsonRPCtoRPCBridge extends AbstractJsonRPCComponent implements DOMRpcService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCtoRPCBridge.class);
    private final Collection<DOMRpcIdentifier> availableRpcs;
    private final Map<String, RpcState> mappedRpcs;
    private static final DOMRpcResult EMPTY = new DefaultDOMRpcResult(Collections.emptySet());
    private volatile boolean shuttingDown = false;

    /**
     * Instantiates a new RPC Bridge.
     *
     * @param peer the peer name
     * @param schemaContext the schema context
     * @param pathMap endpoint mapping
     * @param governance additional json rpc service to query for endpoints
     * @param transportFactory - JSON RPC 2.0 transport factory
     */
    public JsonRPCtoRPCBridge(@NonNull Peer peer, @NonNull SchemaContext schemaContext,
            @NonNull HierarchicalEnumMap<JsonElement, DataType, String> pathMap, @Nullable RemoteGovernance governance,
            @NonNull TransportFactory transportFactory, @NonNull JsonConverter jsonConverter) {
        super(schemaContext, transportFactory, pathMap, jsonConverter, peer);
        Util.populateFromEndpointList(pathMap, peer.nonnullRpcEndpoints().values(), DataType.RPC);
        final ImmutableList.Builder<DOMRpcIdentifier> availableRpcsBuilder = ImmutableList.builder();
        final ImmutableMap.Builder<String, RpcState> mappedRpcsBuilder = ImmutableMap.builder();
        for (final RpcDefinition def : schemaContext.getOperations()) {
            addRpcDefinition(governance, def, availableRpcsBuilder, mappedRpcsBuilder);
        }

        mappedRpcs = mappedRpcsBuilder.build();
        availableRpcs = availableRpcsBuilder.build();

        if (mappedRpcs.isEmpty()) {
            LOG.info("No RPCs to map for {}", peer.getName());
        } else {
            LOG.info("RPC bridge instantiated for '{}' with {} methods", peer.getName(), mappedRpcs.size());
        }
    }

    private void addRpcDefinition(RemoteGovernance governance, RpcDefinition def,
            ImmutableList.Builder<DOMRpcIdentifier> available, ImmutableMap.Builder<String, RpcState> mapped) {
        final QNameModule qm = def.getQName().getModule();
        final String localName = def.getQName().getLocalName();
        final Optional<Module> possibleModule = schemaContext.findModule(qm.getNamespace(), qm.getRevision());
        final JsonObject path = createRootPath(possibleModule.get(), def.getQName());
        final String endpoint = getEndpoint(DataType.RPC, governance, path);
        if (endpoint != null) {
            LOG.info("RPC '{}' mapped to {}", localName, endpoint);
            mapped.put(def.getQName().getLocalName(), new RpcState(localName, def, endpoint, transportFactory));
            available.add(DOMRpcIdentifier.create(def.getPath()));
        } else {
            LOG.warn("RPC '{}' cannot be mapped, no known endpoint", localName);
        }
    }

    private boolean isNotEmpty(ContainerSchemaNode arg) {
        return !arg.getChildNodes().isEmpty();
    }

    /* RPC Bridge functionality */
    @NonNull
    @Override
    public ListenableFuture<DOMRpcResult> invokeRpc(@NonNull final SchemaPath type,
            @Nullable final NormalizedNode<?, ?> input) {
        if (shuttingDown) {
            return bridgeNotAvailable();
        }
        return Futures.immediateFuture(doInvokeRpc(type, input));
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private DOMRpcResult doInvokeRpc(SchemaPath type, NormalizedNode<?, ?> input) {
        final QName rpcQName = type.getLastComponent();
        try {
            final RpcState rpcState = mappedRpcs.get(rpcQName.getLocalName());
            Preconditions.checkArgument(rpcState != null, "Unknown rpc %s, available rpcs: %s", rpcQName,
                    mappedRpcs.keySet());
            final JsonObject jsonForm = input != null
                    ? jsonConverter.rpcConvert(rpcState.rpc().getInput().getPath(), (ContainerNode) input)
                    : new JsonObject();
            final JsonElement jsonResult = rpcState.sendRequest(jsonForm, new JsonObject());
            if (rpcState.lastError() == null) {
                if (isNotEmpty(rpcState.rpc().getOutput())) {
                    return extractResult(rpcState, jsonResult);
                } else {
                    return EMPTY;
                }
            } else {
                return new DefaultDOMRpcResult(RpcResultBuilder.newError(ErrorType.PROTOCOL, null,
                        rpcState.lastError().getMessage(), null, null, null));
            }
        } catch (Exception e) {
            LOG.warn("Invocation of RPC '{}' failed", rpcQName.getLocalName(), e);
            return new DefaultDOMRpcResult(
                    RpcResultBuilder.newError(ErrorType.APPLICATION, null, e.getMessage(), null, null, e.getCause()));
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private DOMRpcResult extractResult(RpcState rpcState, JsonElement jsonResult) throws IOException {
        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> builder = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(NodeIdentifier.create(rpcState.rpc().getOutput().getQName()));

        try (NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(builder)) {
            return extractResultInternal(rpcState, jsonResult, builder, streamWriter);
        }
    }

    private DOMRpcResult extractResultInternal(RpcState rpcState, JsonElement jsonResult,
            DataContainerNodeBuilder<NodeIdentifier, ContainerNode> resultBuilder,
            NormalizedNodeStreamWriter streamWriter) throws IOException {
        final JsonElement wrapper;
        if (jsonResult == null || jsonResult.isJsonNull()) {
            return EMPTY;
        }
        if (jsonResult.isJsonPrimitive()) {
            // wrap primitive type into object
            final JsonObject obj = new JsonObject();
            obj.add(rpcState.rpc().getOutput().getChildNodes().iterator().next().getQName().getLocalName(), jsonResult);
            wrapper = obj;
        } else {
            wrapper = jsonResult;
        }
        try (JsonParserStream jsonParser = JsonParserStream.create(streamWriter,
                JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.getShared(schemaContext),
                rpcState.rpc().getOutput())) {
            jsonParser.parse(JsonReaderAdapter.from(wrapper));
            return new DefaultDOMRpcResult(resultBuilder.build());
        }
    }

    private FluentFuture<DOMRpcResult> bridgeNotAvailable() {
        return FluentFutures
                .immediateFluentFuture(resultFromException(new IllegalStateException("RPC Bridge shutting down")));
    }

    private DOMRpcResult resultFromException(Exception ex) {
        return new DefaultDOMRpcResult(RpcResultBuilder.newError(ErrorType.RPC, "internal-error", ex.getMessage()));
    }

    @NonNull
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
        this.shuttingDown = true;
        mappedRpcs.values().stream().forEach(RpcState::close);
    }
}
