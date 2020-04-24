/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage.Builder;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.opendaylight.jsonrpc.bus.messagelib.RequestMessageHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ResponseErrorCode;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler that receive inbound RPC request and invoke implementation method using reflection. Result is then
 * serialized and sent back to requester.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 20, 2018
 */
public class InboundHandler<T extends RpcService> extends AbstractHandler<T> implements RequestMessageHandler {
    private static final String INPUT = "input";
    private static final Logger LOG = LoggerFactory.getLogger(InboundHandler.class);
    private final T impl;

    public InboundHandler(Class<T> type, RpcInvocationAdapter adapter, T impl) {
        super(type, adapter);
        this.impl = Objects.requireNonNull(impl);
    }

    private Optional<Entry<RpcDefinition, Method>> findMethod(String methodName) {
        return rpcMethodMap.inverse()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().getQName().getLocalName().equals(methodName))
                .findFirst();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public void handleRequest(JsonRpcRequestMessage request, Builder replyBuilder) {
        try {
            final Entry<RpcDefinition, Method> rpcDefEntry = findMethod(request.getMethod())
                    .orElseThrow(() -> new NoSuchMethodError(request.getMethod()));
            final JsonObject wrapper;
            // handle case when requester sent positional arguments to us (in form of JsonArray).
            if (request.getParams() instanceof JsonArray) {
                wrapper = wrapArrayInput((JsonArray) request.getParams(), rpcDefEntry.getKey());
            } else if (request.getParams() instanceof JsonPrimitive) {
                wrapper = wrapPrimitiveInput((JsonPrimitive) request.getParams(), rpcDefEntry.getKey());
            } else {
                wrapper = new JsonObject();
                wrapper.add(INPUT, request.getParams());
            }
            final Method method = rpcDefEntry.getValue();
            final Object[] args = convertArguments(rpcDefEntry, wrapper, method);
            @SuppressWarnings("unchecked")
            final Future<RpcResult<Object>> output = (Future<RpcResult<Object>>) method.invoke(impl, args);
            LOG.debug("Output : {}", output);
            final RpcResult<Object> rpcResult = Futures.getUnchecked(output);
            if (rpcResult.isSuccessful()) {
                if (rpcResult.getResult() != null) {
                    final ContainerNode domData = adapter.codec()
                            .toNormalizedNodeRpcData((DataContainer) rpcResult.getResult());
                    final JsonObject reply = adapter.converter()
                            .get()
                            .rpcConvert(rpcDefEntry.getKey().getOutput().getPath(), domData);
                    replyBuilder.result(reply);
                }
            } else {
                mapRpcError(replyBuilder, rpcResult);
            }
        } catch (NoSuchMethodError e) {
            logRpcInvocationFailure(e);
            replyBuilder.error(new JsonRpcErrorObject(ResponseErrorCode.MethodNotFound.getIntValue(),
                    "No such method : " + e.getMessage(), JsonNull.INSTANCE));
        } catch (IllegalArgumentException e) {
            logRpcInvocationFailure(e);
            replyBuilder.error(new JsonRpcErrorObject(ResponseErrorCode.InvalidParams.getIntValue(), e.getMessage(),
                    JsonNull.INSTANCE));
        } catch (Exception e) {
            // maybe add more sophisticated error mapping?
            logRpcInvocationFailure(e);
            replyBuilder.error(new JsonRpcErrorObject(ResponseErrorCode.InternalError.getIntValue(), e.getMessage(),
                    JsonNull.INSTANCE));
        }
    }

    private void mapRpcError(Builder replyBuilder, final RpcResult<Object> rpcResult) {
        final Collection<RpcError> errors = rpcResult.getErrors();
        if (errors.isEmpty()) {
            replyBuilder.error(new JsonRpcErrorObject(new JsonPrimitive("No error info available")));
        } else if (errors.size() == 1) {
            final RpcError error = errors.iterator().next();
            replyBuilder.error(new JsonRpcErrorObject(mapError(error)));
        } else {
            final JsonArray arr = new JsonArray(errors.size());
            errors.stream().map(this::mapError).forEach(arr::add);
            replyBuilder.error(new JsonRpcErrorObject(arr));
        }
    }

    private Object[] convertArguments(final Entry<RpcDefinition, Method> rpcDefEntry, final JsonObject wrapper,
            final Method method) {
        final Object[] args;
        if (method.getParameterCount() == 1) {
            if (wrapper.get(INPUT).isJsonNull()) {
                wrapper.add(INPUT, new JsonObject());
            }
            final NormalizedNode<?, ?> nn = adapter.converter().get().rpcOutputConvert(rpcDefEntry.getKey(), wrapper);
            final DataObject dataObject = adapter.codec()
                    .fromNormalizedNodeRpcData(rpcDefEntry.getKey().getInput().getPath(), (ContainerNode) nn);
            LOG.debug("Input : {}", dataObject);
            args = new Object[] { dataObject };
        } else {
            args = null;
        }
        return args;
    }

    private void logRpcInvocationFailure(Throwable cause) {
        LOG.error("RPC invocation failed", cause);
    }

    private JsonObject wrapPrimitiveInput(JsonPrimitive request, RpcDefinition rpcDef) {
        final JsonObject wrapper = new JsonObject();
        DataSchemaNode node = rpcDef.getInput().getChildNodes().iterator().next();
        JsonObject prop = new JsonObject();
        prop.add(node.getQName().getLocalName(), request);
        wrapper.add(INPUT, prop);
        return wrapper;
    }

    private JsonObject wrapArrayInput(JsonArray inputArr, RpcDefinition rpcDef) {
        final JsonObject wrapper = new JsonObject();
        final JsonObject prop = new JsonObject();
        final Iterator<DataSchemaNode> it = rpcDef.getInput().getChildNodes().iterator();
        int counter = 0;
        if (inputArr.size() > rpcDef.getInput().getChildNodes().size()) {
            LOG.warn("Extra parameter(s) provided, expected : {}, given : {}", rpcDef.getInput().getChildNodes().size(),
                    inputArr.size());
        }
        while (it.hasNext() && counter < inputArr.size()) {
            prop.add(it.next().getQName().getLocalName(), inputArr.get(counter++));
        }
        wrapper.add(INPUT, prop);
        return wrapper;
    }

    private JsonElement mapError(RpcError rpcError) {
        final JsonObject wrapper = new JsonObject();
        final JsonObject data = new JsonObject();
        wrapper.add("data", data);
        wrapper.add("code", new JsonPrimitive(ResponseErrorCode.InternalError.getIntValue()));
        wrapper.add("message",
                rpcError.getMessage() == null ? JsonNull.INSTANCE : new JsonPrimitive(rpcError.getMessage()));
        return wrapper;
    }

    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
        // NOOP, not used
        return null;
    }

    /*
     * Used by MultiModelRequestDispatcher
     */
    boolean hasMethod(String methodName) {
        return findMethod(methodName).isPresent();
    }
}
