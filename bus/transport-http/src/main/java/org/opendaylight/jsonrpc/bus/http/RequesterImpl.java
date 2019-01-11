/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.util.concurrent.DefaultProgressivePromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;

import org.opendaylight.jsonrpc.bus.api.RecoverableTransportException;
import org.opendaylight.jsonrpc.bus.api.Requester;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.spi.AbstractChannelInitializer;
import org.opendaylight.jsonrpc.bus.spi.CommonConstants;

/**
 * Implementation of {@link Requester} session.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 12, 2018
 */
class RequesterImpl extends AbstractClientSession implements Requester {
    RequesterImpl(String uri, int defaultPort, Bootstrap clientBootstrap, AbstractChannelInitializer channelInitializer,
            boolean isWebsocket) {
        super(uri, defaultPort, clientBootstrap, channelInitializer, isWebsocket, SessionType.REQ);
        connectInternal();
    }

    @Override
    public Future<String> send(String message) {
        if (!isReady()) {
            throw new RecoverableTransportException(sessionType, uri.toString());
        }
        final DefaultProgressivePromise<String> promise = new DefaultProgressivePromise<>(GlobalEventExecutor.INSTANCE);
        channelFuture.channel().attr(CommonConstants.ATTR_RESPONSE_QUEUE).get().set(promise);
        channelFuture.channel().writeAndFlush(HttpUtil.createRequestObject(isWebsocket, message));
        return promise;
    }
}
