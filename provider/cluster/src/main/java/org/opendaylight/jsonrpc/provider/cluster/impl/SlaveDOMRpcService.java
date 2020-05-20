/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.impl;

import static akka.pattern.Patterns.ask;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.util.Timeout;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.provider.cluster.messages.InvokeRpcRequest;
import org.opendaylight.mdsal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DOMRpcService} implementation that forwards RPC invocation requests to master actor in cluster.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 7, 2020
 */
public class SlaveDOMRpcService implements DOMRpcService {
    private static final Logger LOG = LoggerFactory.getLogger(SlaveDOMRpcService.class);
    private final ActorRef masterActorRef;
    private final Timeout timeout;
    private final ActorSystem actorSystem;
    private final String peer;

    public SlaveDOMRpcService(ActorSystem actorSystem, Timeout timeout, ActorRef masterActorRef, String peer) {
        this.actorSystem = actorSystem;
        this.timeout = timeout;
        this.masterActorRef = masterActorRef;
        this.peer = peer;
    }

    @Override
    public @NonNull ListenableFuture<? extends DOMRpcResult> invokeRpc(@NonNull SchemaPath type,
            @NonNull NormalizedNode<?, ?> input) {
        LOG.debug("[{}] invoke '{}' using {}", peer, type.getLastComponent().getLocalName(), input);
        final SettableFuture<DOMRpcResult> result = SettableFuture.create();
        ask(masterActorRef, InvokeRpcRequest.create(type, input), timeout)
                .onComplete(CompletionAdapters.rpcInvocation(result, peer), actorSystem.dispatcher());
        return FluentFuture.from(result);
    }

    @Override
    public <T extends DOMRpcAvailabilityListener> @NonNull ListenerRegistration<T> registerRpcListener(
            @NonNull T listener) {
        throw new UnsupportedOperationException("registerRpcListener is not supported in cluster");
    }
}
