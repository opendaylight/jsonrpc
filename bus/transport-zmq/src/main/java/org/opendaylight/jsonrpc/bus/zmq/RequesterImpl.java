/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import io.netty.bootstrap.Bootstrap;
import io.netty.util.concurrent.DefaultProgressivePromise;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.RecoverableTransportException;
import org.opendaylight.jsonrpc.bus.api.Requester;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.spi.AbstractReconnectingClient;
import org.opendaylight.jsonrpc.bus.spi.CommonConstants;

/**
 * Implementation of {@link Requester} session type.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 7, 2018
 */
final class RequesterImpl extends AbstractReconnectingClient implements Requester {
    RequesterImpl(String uri, Bootstrap bootstrap, MessageListener listener, EventExecutorGroup handlerExecutor) {
        super(uri, 10000, bootstrap, new ClientInitializer(SessionType.REQ, handlerExecutor, listener),
                SessionType.REQ);
        connectInternal();
    }

    @Override
    public Future<String> send(String message) {
        if (!isReady()) {
            throw new RecoverableTransportException(sessionType, uri.toString());
        }
        final DefaultProgressivePromise<String> promise = new DefaultProgressivePromise<>(GlobalEventExecutor.INSTANCE);
        channelFuture.channel().attr(CommonConstants.ATTR_RESPONSE_QUEUE).get().set(promise);
        channelFuture.channel().writeAndFlush(Util.serializeMessage(message));
        return promise;
    }
}
