/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.model.RpcExceptionImpl;
import org.opendaylight.jsonrpc.model.RpcState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JsonRPCtoRPCBridge implements DOMRpcService, AutoCloseable {
    private static final int MAX_QUEUE_DEPTH = 64;
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCtoRPCBridge.class);
    private final SchemaContext schemaContext;
    private final JsonConverter jsonConverter;
    private final Collection<DOMRpcIdentifier> availableRpcs = new ArrayList<>();
    private final Map<String, RpcState> mappedRpcs = new HashMap<>();
    private final ArrayBlockingQueue<JsonRPCDOMRpcResultFuture> requestQueue;
    private final Thread requestProcessorThread;
    private boolean shuttingDown = false;

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
    public JsonRPCtoRPCBridge(@Nonnull Peer peer, @Nonnull SchemaContext schemaContext,
            @Nonnull HierarchicalEnumMap<JsonElement, DataType, String> pathMap, @Nonnull RemoteGovernance governance,
            @Nullable TransportFactory transportFactory) throws URISyntaxException {
        /* Endpoints via configuration */
        if (peer.getRpcEndpoints() != null) {
            Util.populateFromEndpointList(pathMap, peer.getRpcEndpoints(), DataType.RPC);
        }
        Objects.requireNonNull(peer);
        this.schemaContext = Objects.requireNonNull(schemaContext);
        this.jsonConverter = new JsonConverter(schemaContext);
        for (final RpcDefinition def : schemaContext.getOperations()) {
            addRpcDefinition(peer, pathMap, governance, def, transportFactory);
        }
        if (mappedRpcs.isEmpty()) {
            LOG.warn("No RPCs to map for " + peer.getName());
        }
        requestQueue = new ArrayBlockingQueue<>(MAX_QUEUE_DEPTH);
        requestProcessorThread = new Thread(this::requestProcessorThreadLoop);
        requestProcessorThread.start();
        LOG.info("RPC bridge instantiated for {}", peer.getName());
    }

    private void addRpcDefinition(Peer peer, HierarchicalEnumMap<JsonElement, DataType, String> pathMap,
            RemoteGovernance governance, RpcDefinition def, TransportFactory transportFactory)
                    throws URISyntaxException {
        final QNameModule qmodule = def.getQName().getModule();
        final Optional<Module> possibleModule = schemaContext.findModule(qmodule.getNamespace(), qmodule.getRevision());

        Preconditions.checkState(possibleModule.isPresent(), "RPC %s cannot be mapped, module not found",
                def.getQName().getLocalName());

        final StringBuilder sb = new StringBuilder();
        sb.append(possibleModule.get().getName());
        sb.append(":");
        sb.append(def.getQName().getLocalName());
        final String topLevel = sb.toString();
        final JsonObject path = new JsonObject();
        path.add(topLevel, new JsonObject());

        String methodEndpoint = pathMap.lookup(path, DataType.RPC).orElse(null);
        LOG.debug("Method endpoint - map lookup is  {}", methodEndpoint);

        if (methodEndpoint == null) {

            Preconditions.checkNotNull(governance,
                    "Can't create mapping lookup because governance was not provided for peer %s", peer.toString());
            methodEndpoint = governance.governance(-1, peer.getName(), path);
            if (methodEndpoint != null) {
                pathMap.put(path, DataType.RPC, methodEndpoint);
            }
        }

        LOG.debug("Method endpoint - governance+map lookup is  {}", methodEndpoint);
        if (methodEndpoint != null) {
            LOG.debug("RPC {} mapped to {}", topLevel, methodEndpoint);
            mappedRpcs.put(def.getQName().getLocalName(),
                    new RpcState(def.getQName().getLocalName(), def, methodEndpoint, transportFactory));
            availableRpcs.add(DOMRpcIdentifier.create(def.getPath()));
        } else {
            LOG.error("RPC {} cannot be mapped, no known endpoint", topLevel);
        }
    }

    /* Perform a full check if a container node is null or empty
     * ODL schema prior to 05 March 2017 returns nulls for empty
     * input or output rpc statements. After that it returns
     * empty containers.
     * */
    private boolean isNotEmpty(ContainerSchemaNode arg) {
        if (arg == null) {
            return false;
        }
        if (arg.getChildNodes().isEmpty()) {
            return false;
        }
        return true;
    }

    /* RPC Bridge functionality */
    @Nonnull
    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(@Nonnull final SchemaPath type,
            @Nullable final NormalizedNode<?, ?> input) {

        if (this.shuttingDown) {
            return Futures.immediateFailedCheckedFuture(new RpcExceptionImpl("RPC Bridge shutting down"));
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
            return Futures.immediateFailedCheckedFuture(new RpcExceptionImpl(e.getMessage()));
        }
        return postponedResult;
    }

    /* Dequeue element (used by the RPC worker thread)#
     *
     * @return next QueueElement to process or block until one becomes available
     *
     * */

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
    public void doInvokeRpc(JsonRPCDOMRpcResultFuture request) {
        final QName rpcQName = request.getType().getLastComponent();
        JsonObject jsonForm = null;
        try {
            RpcState rpcState = Preconditions.checkNotNull(mappedRpcs.get(rpcQName.getLocalName()),
                    "Unknown rpc %s, available rpcs: %s", rpcQName, mappedRpcs.keySet());
            if (!request.isPollingForResult()) {
                if (isNotEmpty(rpcState.rpc().getInput())) {
                    Preconditions.checkArgument(request.getInput() instanceof ContainerNode,
                            "Transforming an rpc with input: %s, payload has to be a container, but was: %s",
                            rpcQName, request.getInput());
                    jsonForm = jsonConverter.rpcConvert(rpcState.rpc().getInput().getPath(),
                            (ContainerNode) request.getInput());
                }
            }
            JsonElement jsonResult;
            jsonResult = rpcState.sendRequest(jsonForm, request.formMetadata());
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
                    if ((jsonResult == null || jsonResult.isJsonNull()) && rpcState.lastMetadata() != null) {
                    /* we performed an async handle request - someone wants an answer, requeue to continue polling */
                        if (! requestQueue.offer(request)) {
                            request.setException(new RpcExceptionImpl("Queue Full"));
                        }
                        return;
                    }
                }

                final DOMRpcResult toODL;
                if (isNotEmpty(rpcState.rpc().getOutput())) {
                    final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> resultBuilder =
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

    public void kick(JsonRPCDOMRpcResultFuture request) {
        request.startPollingForResult();
        if (! requestQueue.offer(request)) {
            LOG.error("Failed to requeue UUID {}", request.getUuid().toString());
            request.set(null);
            request.setException(new RpcExceptionImpl("Queue Full"));
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private DOMRpcResult extractResult(RpcState rpcState, JsonElement jsonResult,
            DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> resultBuilder) {
        try (NormalizedNodeStreamWriter streamWriter = Util
                .wrapWithAnyXmlNullValueCallBack(ImmutableNormalizedNodeStreamWriter.from(resultBuilder))) {
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
            DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> resultBuilder,
            NormalizedNodeStreamWriter streamWriter) {
        try (JsonParserStream jsonParser = JsonParserStream.create(streamWriter, schemaContext,
                rpcState.rpc().getOutput())) {
            jsonParser.parse(new JsonReader(new StringReader(jsonResult.toString())));
            return new DefaultDOMRpcResult(resultBuilder.build());
        } catch (IOException e) {
            LOG.error("Failed to process JSON", e);
            return resultFromException(e);
        }
    }

    private DOMRpcResult resultFromException(Exception ex) {
        return new DefaultDOMRpcResult(RpcResultBuilder.newError(ErrorType.RPC, "internal-error", ex.getMessage()));
    }

    @Nonnull
    @Override
    public <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(
            @Nonnull final T listener) {
        LOG.info("registered RPC implementation for json rpc broker");
        listener.onRpcAvailable(availableRpcs);

        return new ListenerRegistration<T>() {
            @Override
            public void close() {
                // NOOP, no rpcs appear and disappear in this implementation
            }

            @Override
            public T getInstance() {
                return listener;
            }
        };
    }

    @Override
    public void close() {
        this.shuttingDown = true;
        try {
            requestProcessorThread.join(500); /* half a second */
            requestProcessorThread.interrupt();
            requestProcessorThread.join();
        } catch (java.lang.InterruptedException e) {
            // Do nothing - this gets us out of the loop
        }
        mappedRpcs.clear();
    }

    private void requestProcessorThreadLoop() {
        try {
            while (opStatus()) {
                doInvokeRpc(deQueue());
            }
        } catch (InterruptedException e) {
            flushQueue();
        }
    }

    public boolean opStatus() {
        return ! this.shuttingDown;
    }
}
