/*
 * Copyright (c) 2020 dNation.cloud. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import static org.opendaylight.yangtools.util.concurrent.FluentFutures.immediateFluentFuture;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.opendaylight.jsonrpc.bus.api.RpcMethod;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClient implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(RpcClient.class);
    private final Class<? extends AutoCloseable> type;
    private final TransportFactory factory;
    private final String endpoint;
    private final RpcDefinition def;
    private final Method method;
    private final Codec<JsonElement, ContainerNode, IOException> outputCodec;
    private final Codec<JsonElement, ContainerNode, IOException> inputCodec;

    public RpcClient(JsonRpcCodecFactory codecFactory, RpcDefinition def, TransportFactory factory, String endpoint) {
        type = InterfaceGenerator.generate(def);
        this.factory = factory;
        this.endpoint = endpoint;
        this.def = def;
        method = Arrays.asList(type.getDeclaredMethods())
                .stream()
                .filter(m -> m.getDeclaredAnnotation(RpcMethod.class) != null)
                .filter(m -> m.getDeclaredAnnotation(RpcMethod.class).value().equals(def.getQName().getLocalName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find method in generated API"));
        inputCodec = codecFactory.rpcInputCodec(def);
        outputCodec = codecFactory.rpcOutputCodec(def);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    public ListenableFuture<DOMRpcResult> invoke(NormalizedNode<?, ?> input) {
        LOG.debug("Invoking RPC '{}' using endpoint {}", def.getQName().getLocalName(), endpoint);
        try (AutoCloseable client = factory.endpointBuilder().requester().createProxy(type, endpoint)) {
            final Object output;
            if (method.getParameterCount() == 0) {
                output = method.invoke(client);
            } else {
                output = method.invoke(client, inputCodec.serialize((ContainerNode) input));
            }
            return immediateFluentFuture(new DefaultDOMRpcResult(outputCodec.deserialize((JsonObject) output)));
        } catch (Exception e) {
            final RpcError error = RpcResultBuilder.newError(ErrorType.RPC, "jsonrpc",
                    String.format("Invocation of method '%s' failed", def.getQName().getLocalName()), null, endpoint,
                    e);
            return immediateFluentFuture(new DefaultDOMRpcResult(error));
        }
    }

    @Override
    public void close() {
        // Currently NOOP
    }
}
