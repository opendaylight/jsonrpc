/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import org.opendaylight.jsonrpc.bus.spi.CommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler which perform handshake.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 23, 2018
 */
public class WebSocketClientHandshake extends SimpleChannelInboundHandler<Object> {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketClientHandshake.class);

    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture = null;

    public WebSocketClientHandshake(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        final Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            try {
                handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                // signal handshake completed
                ctx.channel().attr(CommonConstants.ATTR_HANDSHAKE_DONE).set(true);
                LOG.debug("Client connected on {}", ch);
                handshakeFuture.setSuccess();
                // remove this handler from pipeline
                ctx.pipeline().remove(this);
            } catch (WebSocketHandshakeException e) {
                LOG.error("Handshake failed", e);
                handshakeFuture.setFailure(e);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }
}
