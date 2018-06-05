/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;

import java.util.Objects;

import org.opendaylight.jsonrpc.security.api.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler which delegates authorization to given {@link AuthenticationProvider}. It is used on both Websocket and HTTP
 * transports. Note that only first request is authenticated. This handler should be present in pipeline only if server
 * requires clients to be authenticated.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jun 6, 2018
 */
public class ServerAuthHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(ServerAuthHandler.class);
    private final AuthenticationProvider authenticationProvider;

    public ServerAuthHandler(final AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = Objects.requireNonNull(authenticationProvider);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        // assume worst at first
        boolean passed = false;
        if (msg.headers().contains(HttpHeaderNames.AUTHORIZATION)) {
            final String[] auth = HttpUtil.parseBasicAuthHeader(msg.headers().get(HttpHeaderNames.AUTHORIZATION));
            passed = authenticationProvider.validate(auth[0], auth[1]);
            LOG.debug("Channel {} authenticated? {}", ctx.channel(), passed);
        }

        if (!passed) {
            // send HTTP401 back to client and close connection
            LOG.warn("Channel {} not authenticated, sending HTTP401", ctx.channel());
            ctx.channel().writeAndFlush(HttpUtil.createForbidenResponse()).addListener(ChannelFutureListener.CLOSE);
        } else {
            // retain reference count on request so that next handler in pipeline can handle it
            // afterwards
            msg.retain();
            // once channel is authenticated, we are no longer needed
            LOG.debug("Removing authentication handler from pipeline on channel {}", ctx.channel());
            ctx.channel().pipeline().remove(this);
            ctx.fireChannelRead(msg);
        }
    }
}
