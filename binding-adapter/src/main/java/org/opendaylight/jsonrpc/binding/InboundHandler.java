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
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Objects;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage.Builder;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.opendaylight.jsonrpc.bus.messagelib.RequestMessageHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ResponseErrorCode;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.Rpc;
import org.opendaylight.yangtools.binding.RpcInput;
import org.opendaylight.yangtools.binding.RpcOutput;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler that receive inbound RPC request and invoke implementation method using reflection. Result is then
 * serialized and sent back to requester.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 20, 2018
 */
public class InboundHandler<T extends Rpc<?, ?>> extends AbstractHandler<T> implements RequestMessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(InboundHandler.class);
    private final Rpc<?, ?> impl;

    public InboundHandler(RpcInvocationAdapter adapter, T impl) {
        super((Class<T>) impl.implementedInterface(), adapter);
        this.impl = Objects.requireNonNull(impl);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public void handleRequest(JsonRpcRequestMessage request, Builder replyBuilder) {
        final var method = request.getMethod();
        if (!hasMethod(method)) {
            logRpcInvocationFailure(new NoSuchMethodError(method));
            replyBuilder.error(new JsonRpcErrorObject(ResponseErrorCode.MethodNotFound.getIntValue(),
                "No such method : " + method, JsonNull.INSTANCE));
            return;
        }

        try {
            final var arg = convertArguments(request.getParams());
            @SuppressWarnings("unchecked")
            final var output = ((Rpc<RpcInput, RpcOutput>) impl).invoke(arg);
            LOG.debug("Output : {}", output);
            final var rpcResult = Futures.getUnchecked(output);
            if (rpcResult.isSuccessful()) {
                final var result = rpcResult.getResult();
                if (result != null) {
                    final ContainerNode domData = adapter.codec().toNormalizedNodeRpcData(result);
                    final JsonElement reply = adapter.converter()
                            .get()
                            .rpcOutputCodec(rpcDef)
                            .serialize(domData);
                    replyBuilder.result(reply);
                }
            } else {
                mapRpcError(replyBuilder, rpcResult);
            }
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

    private static void mapRpcError(Builder replyBuilder, final RpcResult<?> rpcResult) {
        final var errors = rpcResult.getErrors();
        if (errors.isEmpty()) {
            replyBuilder.error(new JsonRpcErrorObject(new JsonPrimitive("No error info available")));
        } else if (errors.size() == 1) {
            final RpcError error = errors.iterator().next();
            replyBuilder.error(new JsonRpcErrorObject(mapError(error)));
        } else {
            final JsonArray arr = new JsonArray(errors.size());
            errors.stream().map(InboundHandler::mapError).forEach(arr::add);
            replyBuilder.error(new JsonRpcErrorObject(arr));
        }
    }

    private RpcInput convertArguments(final JsonElement wrapper) throws IOException {
        final ContainerNode nn = adapter.converter()
            .get()
            .rpcInputCodec(rpcDef)
            .deserialize(wrapper);
        final DataObject dataObject = adapter.codec()
            .fromNormalizedNodeRpcData(Absolute.of(rpcDef.getQName(), rpcDef.getInput().getQName()), nn);
        LOG.debug("Input : {}", dataObject);
        return RpcInput.class.cast(dataObject);
    }

    private static void logRpcInvocationFailure(Throwable cause) {
        LOG.error("RPC invocation failed", cause);
    }

    private static JsonElement mapError(RpcError rpcError) {
        final JsonObject wrapper = new JsonObject();
        final JsonObject data = new JsonObject();
        wrapper.add("data", data);
        wrapper.add("code", new JsonPrimitive(ResponseErrorCode.InternalError.getIntValue()));
        wrapper.add("message",
                rpcError.getMessage() == null ? JsonNull.INSTANCE : new JsonPrimitive(rpcError.getMessage()));
        return wrapper;
    }

    @Override
    protected Object doHandleInvocation(Object proxy, Method method, Object[] args) {
        // NOOP, not used
        return null;
    }

    /*
     * Used by MultiModelRequestDispatcher
     */
    boolean hasMethod(String methodName) {
        return rpcDef.getQName().getLocalName().equals(methodName);
    }
}
