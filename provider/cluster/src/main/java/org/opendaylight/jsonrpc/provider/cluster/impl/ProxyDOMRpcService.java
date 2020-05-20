/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.impl;

import static akka.pattern.Patterns.ask;
import static org.opendaylight.jsonrpc.provider.cluster.impl.ClusterUtil.DEFAULT_RPC_TIMEOUT;
import static org.opendaylight.jsonrpc.provider.cluster.impl.ClusterUtil.durationFromUint16seconds;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.OnComplete;
import akka.util.Timeout;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.model.RpcExceptionImpl;
import org.opendaylight.jsonrpc.provider.cluster.messages.PathAndDataMsg;
import org.opendaylight.jsonrpc.provider.cluster.rpc.EmptyRpcResponse;
import org.opendaylight.jsonrpc.provider.cluster.rpc.InvokeRpcRequest;
import org.opendaylight.jsonrpc.provider.cluster.rpc.InvokeRpcResponse;
import org.opendaylight.mdsal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 * {@link DOMRpcService} implementation that forwards RPC invocation requests to master actor in cluster.
 *
 * <p>
 * Acknowledgement : this code is inspired by implementation of netconf-topology-singleton.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 7, 2020
 */
class ProxyDOMRpcService implements DOMRpcService {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyDOMRpcService.class);
    private final ActorRef masterActorRef;
    private final Timeout askTimeout;
    private final ActorSystem actorSystem;
    private final Peer peer;

    ProxyDOMRpcService(Peer peer, ActorRef masterActorRef, ClusterDependencies dependencies) {
        final Duration askDuration = dependencies.getConfig() == null ? DEFAULT_RPC_TIMEOUT
                : durationFromUint16seconds(dependencies.getConfig().getRpcResponseWaitTime(), DEFAULT_RPC_TIMEOUT);
        askTimeout = Timeout.apply(askDuration.toSeconds(), TimeUnit.SECONDS);
        this.masterActorRef = masterActorRef;
        this.peer = peer;
        this.actorSystem = dependencies.getActorSystem();
        LOG.debug("Created {}", this);
    }

    @Override
    public @NonNull ListenableFuture<? extends DOMRpcResult> invokeRpc(@NonNull SchemaPath type,
            @NonNull NormalizedNode<?, ?> input) {
        LOG.debug("[{}] invoke '{}' using {}", peer.getName(), type.getLastComponent().getLocalName(), input);
        final SettableFuture<DOMRpcResult> result = SettableFuture.create();
        final InvokeRpcRequest request = InvokeRpcRequest.create(type, input);
        LOG.debug("Sending {} to {}", request, masterActorRef);
        ask(masterActorRef, request, askTimeout).onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object response) {
                if (failure != null) {
                    if (failure instanceof RpcExceptionImpl) {
                        result.setException(failure);
                    } else {
                        result.setException(
                                new RpcExceptionImpl(String.format("%s : RPC invocation failed", peer), failure));
                    }
                    return;
                }

                if (response instanceof EmptyRpcResponse) {
                    result.set(null);
                    return;
                }

                final Collection<? extends RpcError> errors = ((InvokeRpcResponse) response).getErrors();
                final PathAndDataMsg responseData = ((InvokeRpcResponse) response).getData();
                final DOMRpcResult rpcResult;
                if (responseData == null) {
                    rpcResult = new DefaultDOMRpcResult(ImmutableList.copyOf(errors));
                } else {
                    rpcResult = new DefaultDOMRpcResult(responseData.getData(), errors);
                }
                result.set(rpcResult);
            }

        }, actorSystem.dispatcher());
        LOG.debug("[{}] invocation result {}", peer.getName(), result);
        return FluentFuture.from(result);
    }

    @Override
    public <T extends DOMRpcAvailabilityListener> @NonNull ListenerRegistration<T> registerRpcListener(
            @NonNull T listener) {
        throw new UnsupportedOperationException("registerRpcListener is not supported in cluster");
    }

    @Override
    public String toString() {
        return "ProxyDOMRpcService [peer=" + peer.getName() + ", askTimeout=" + askTimeout + ", masterActorRef="
                + masterActorRef + "]";
    }
}
