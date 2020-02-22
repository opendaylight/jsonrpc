/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.JsonReaderAdapter;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.model.RpcExceptionImpl;
import org.opendaylight.jsonrpc.model.RpcState;
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

public final class JsonRPCtoRPCBridge extends AbstractJsonRPCComponent
        implements DOMRpcService, AutoCloseable, Consumer<JsonRPCDOMRpcResultFuture> {
    private static final int MAX_QUEUE_DEPTH = 64;
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCtoRPCBridge.class);
    private final Collection<DOMRpcIdentifier> availableRpcs;
    private final Map<String, RpcState> mappedRpcs;
    private final BlockingQueue<JsonRPCDOMRpcResultFuture> requestQueue = new ArrayBlockingQueue<>(MAX_QUEUE_DEPTH);
    private final Future<?> processorFuture;
    private final ExecutorService executorService;
    private volatile boolean shuttingDown = false;

    /**
     * Instantiates a new RPC Bridge.
     *
     * @param peer the peer name
     * @param schemaContext the schema context
     * @param pathMap endpoint mapping
     * @param governance additional json rpc service to query for endpoints
     * @param transportFactory - JSON RPC 2.0 transport factory
     * @throws URISyntaxException internal error
     */
    public JsonRPCtoRPCBridge(@NonNull Peer peer, @NonNull SchemaContext schemaContext,
            @NonNull HierarchicalEnumMap<JsonElement, DataType, String> pathMap, @Nullable RemoteGovernance governance,
            @NonNull TransportFactory transportFactory, @NonNull JsonConverter jsonConverter)
            throws URISyntaxException {
        super(schemaContext, transportFactory, pathMap, jsonConverter, peer);
        Util.populateFromEndpointList(pathMap, peer.getRpcEndpoints(), DataType.RPC);
        final ImmutableList.Builder<DOMRpcIdentifier> availableRpcsBuilder = ImmutableList.builder();
        final ImmutableMap.Builder<String, RpcState> mappedRpcsBuilder = ImmutableMap.builder();
        for (final RpcDefinition def : schemaContext.getOperations()) {
            addRpcDefinition(governance, def, availableRpcsBuilder, mappedRpcsBuilder);
        }

        mappedRpcs = mappedRpcsBuilder.build();
        availableRpcs = availableRpcsBuilder.build();

        if (mappedRpcs.isEmpty()) {
            LOG.info("No RPCs to map for {}", peer.getName());
            executorService = null;
            processorFuture = Futures.immediateFuture(null);
        } else {
            executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                    .setNameFormat("jsonrpc-async-dispatch-" + peer.getName() + "-%d")
                    .build());
            processorFuture = executorService.submit(this::requestProcessorThreadLoop);
        }
        LOG.info("RPC bridge instantiated for '{}' with {} methods", peer.getName(), mappedRpcs.size());
    }

    private void addRpcDefinition(RemoteGovernance governance, RpcDefinition def,
            ImmutableList.Builder<DOMRpcIdentifier> available, ImmutableMap.Builder<String, RpcState> mapped)
            throws URISyntaxException {
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

        SettableFuture<DOMRpcResult> futureResult = SettableFuture.create();
        SettableFuture<String> asyncUUID = SettableFuture.create();
        JsonRPCDOMRpcResultFuture postponedResult = new JsonRPCDOMRpcResultFuture(futureResult, asyncUUID,
                this, type, input);
        try {
            requestQueue.put(
                    postponedResult
            );
        } catch (java.lang.InterruptedException e) {
            return bridgeNotAvailable();
        }
        return postponedResult;
    }

    private FluentFuture<DOMRpcResult> bridgeNotAvailable() {
        return FluentFutures.immediateFluentFuture(resultFromException(
                new IllegalStateException("RPC Bridge shutting down")));
    }

    /**
     * Dequeue element (used by the RPC worker thread).
     *
     * @return next QueueElement to process or block until one becomes available
     *
     */
    public JsonRPCDOMRpcResultFuture deQueue() throws java.lang.InterruptedException {
        return requestQueue.take();
    }

    /* Mark all remaining queue elements in the queue as cancelled
     * provide an exception to be thrown if the upper layers asks for any
     * of them
     * */
    public void flushQueue() {
        JsonRPCDOMRpcResultFuture request = requestQueue.poll();
        while (request != null) {
            request.setException(new RpcExceptionImpl("Execution interrupted due to broker shutdown"));
            request = requestQueue.poll();
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
    public void doInvokeRpc(JsonRPCDOMRpcResultFuture request) {
        final QName rpcQName = request.getType().getLastComponent();
        JsonObject jsonForm = null;
        try {
            final RpcState rpcState = mappedRpcs.get(rpcQName.getLocalName());
            Preconditions.checkArgument(rpcState != null, "Unknown rpc %s, available rpcs: %s", rpcQName,
                        mappedRpcs.keySet());
            if (!request.isPollingForResult() && isNotEmpty(rpcState.rpc().getInput())) {
                Preconditions.checkArgument(request.getInput() instanceof ContainerNode,
                        "Transforming an rpc with input: %s, payload has to be a container, but was: %s",
                        rpcQName, request.getInput());
                jsonForm = jsonConverter.rpcConvert(rpcState.rpc().getInput().getPath(),
                        (ContainerNode) request.getInput());
            }
            final JsonElement jsonResult = rpcState.sendRequest(jsonForm, request.formMetadata());
            if (rpcState.lastError() == null) {
                if (!request.isPollingForResult()) {
                    if (rpcState.lastMetadata() == null) {
                        request.setUuid(null);
                    } else {
                        if (rpcState.lastMetadata().get("async") != null) {
                            request.setUuid(rpcState.lastMetadata().get("async").getAsString());
                            return;
                        } else {
                            LOG.error("Invalid Request Metadata");
                        }
                    }
                } else {
                    if (shouldRequeue(jsonResult, rpcState.lastMetadata())) {
                        if (! requestQueue.offer(request)) {
                            request.setException(new RpcExceptionImpl("Queue Full"));
                        }
                        return;
                    }
                }

                final DOMRpcResult toODL;
                if (isNotEmpty(rpcState.rpc().getOutput())) {
                    final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> resultBuilder =
                            ImmutableContainerNodeBuilder.create().withNodeIdentifier(NodeIdentifier.create(
                                    rpcState.rpc().getOutput().getQName()));
                    toODL = extractResult(rpcState, jsonResult, resultBuilder);
                } else {
                    toODL = new DefaultDOMRpcResult((NormalizedNode<?, ?>) null);
                }
                request.set(toODL);
            } else {
                request.setException(new RpcExceptionImpl(rpcState.lastError().getMessage()));
            }
        } catch (RuntimeException e) {
            request.setException(e);
            return;
        }
    }

    /*
     * we performed an async handle request - someone wants an answer, requeue to continue polling
     */
    @VisibleForTesting
    static boolean shouldRequeue(JsonElement result, JsonObject metadata) {
        return (result == null || result.isJsonNull()) && metadata != null;
    }

    @Override
    public void accept(JsonRPCDOMRpcResultFuture request) {
        request.startPollingForResult();
        if (! requestQueue.offer(request)) {
            LOG.error("Failed to requeue UUID {} because queue is full", request.getUuid());
            request.set(null);
            request.setException(new RpcExceptionImpl("Queue Full"));
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private DOMRpcResult extractResult(RpcState rpcState, JsonElement jsonResult,
            DataContainerNodeBuilder<NodeIdentifier, ContainerNode> resultBuilder) {
        try (NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(resultBuilder)) {
            return extractResultInternal(rpcState, jsonResult, resultBuilder, streamWriter);
        } catch (IOException e1) {
            LOG.error("Failed to close JSON parser", e1);
            return resultFromException(e1);
        } catch (RuntimeException e) {
            LOG.error("Failed invoke RPC method", e);
            return resultFromException(e);
        }
    }

    private DOMRpcResult extractResultInternal(RpcState rpcState, JsonElement jsonResult,
            DataContainerNodeBuilder<NodeIdentifier, ContainerNode> resultBuilder,
            NormalizedNodeStreamWriter streamWriter) {
        final JsonElement wrapper;
        if (jsonResult.isJsonPrimitive()) {
            // wrap primitive type into object
            final JsonObject obj = new JsonObject();
            obj.add(rpcState.rpc().getOutput().getChildNodes().iterator().next().getQName().getLocalName(),
                    jsonResult);
            wrapper = obj;
        } else {
            wrapper = jsonResult;
        }
        try (JsonParserStream jsonParser = JsonParserStream.create(streamWriter,
                JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.getShared(schemaContext),
                rpcState.rpc().getOutput())) {
            jsonParser.parse(JsonReaderAdapter.from(wrapper));
            return new DefaultDOMRpcResult(resultBuilder.build());
        } catch (IOException e) {
            LOG.error("Failed to process JSON", e);
            return resultFromException(e);
        }
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
        processorFuture.cancel(true);
        mappedRpcs.values().stream().forEach(RpcState::close);
    }

    private void requestProcessorThreadLoop() {
        try {
            while (opStatus()) {
                doInvokeRpc(deQueue());
            }
        } catch (InterruptedException e) {
            flushQueue();
            Thread.currentThread().interrupt();
        }
    }

    public boolean opStatus() {
        return ! this.shuttingDown;
    }
}
