/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonElement;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Objects;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.messagelib.RequesterSession;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Rpc;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler that intercept local method invocation and make RPC call to remote responder.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 20, 2018
 */
public class OutboundHandler<T extends Rpc<?, ?>> extends AbstractHandler<T> {
    private static final Logger LOG = LoggerFactory.getLogger(OutboundHandler.class);
    
    private final RequesterSession session;

    public OutboundHandler(Class<T> type, RpcInvocationAdapter adapter, RequesterSession session) {
        super(type, adapter);
        this.session = session;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return handleInvocationInternal(method, args);
        } catch (NullPointerException | IllegalArgumentException e) {
            return Futures.immediateFuture(
                    RpcResultBuilder.<Object>failed().withError(ErrorType.APPLICATION, e.getMessage()).build());
        } catch (Exception e) {
            LOG.error("Error while invoking RPC method", e);
            return Futures.immediateFuture(
                    RpcResultBuilder.<Object>failed().withError(ErrorType.RPC, e.getMessage()).build());
        }
    }

    private Object handleInvocationInternal(Method method, Object[] args) throws IOException {
        Preconditions.checkArgument(args.length < 2, "Unexpected number of arguments : %d", args.length);
        RpcDefinition rpcDef = rpcMethodMap.get(method);
        Objects.requireNonNull(rpcDef);
        final JsonElement request;
        // RPC with input
        if (args.length == 1) {
            // convert binding object into DOM
            final ContainerNode domData = adapter.codec().toNormalizedNodeRpcData((DataContainer) args[0]);
            request = adapter.converter().get().rpcInputCodec(rpcDef).serialize(domData);
            // RPC without input
        } else {
            request = null;
        }
        final JsonRpcReplyMessage reply = session.sendRequestAndReadReply(rpcDef.getQName().getLocalName(), request);
        if (reply.isError()) {
            return Futures.immediateFuture(RpcResultBuilder.<Object>failed()
                    .withError(ErrorType.APPLICATION, reply.getError().getMessage())
                    .build());
        } else {
            final ParameterizedType futureType = (ParameterizedType) method.getGenericReturnType();
            final ParameterizedType rpcResultType = (ParameterizedType) futureType.getActualTypeArguments()[0];
            // void return type
            if (rpcResultType.getActualTypeArguments()[0].equals(Void.class)) {
                return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
            } else {
                final NormalizedNode nn = adapter.converter()
                        .get()
                        .rpcOutputCodec(rpcDef)
                        .deserialize(reply.getResult());
                final DataObject result = adapter.codec()
                        .fromNormalizedNodeRpcData(Absolute.of(rpcDef.getQName(), rpcDef.getOutput().getQName()),
                                (ContainerNode) nn);
                LOG.debug("Deserialized : {}", result);
                return Futures.immediateFuture(RpcResultBuilder.<DataObject>success(result).build());
            }
        }
    }
}
