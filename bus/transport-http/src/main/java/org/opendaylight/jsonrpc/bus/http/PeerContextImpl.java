/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.nio.charset.StandardCharsets;

import org.opendaylight.jsonrpc.bus.api.PeerContext;
import org.opendaylight.jsonrpc.bus.spi.AbstractPeerContext;

/**
 * Transport specific implementation of {@link PeerContext}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 19, 2018
 */
public class PeerContextImpl extends AbstractPeerContext {
    private final boolean isWebsocket;

    public PeerContextImpl(Channel channel, final boolean isWebsocket, final boolean isSsl) {
        super(channel, HttpUtil.getTransport(isWebsocket, isSsl));
        this.isWebsocket = isWebsocket;
    }

    @Override
    public void send(String msg) {
        channel.writeAndFlush(isWebsocket ? getWsResponse(msg) : getHttpResponse(msg));
    }

    private HttpResponse getHttpResponse(String msg) {
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeCharSequence(msg, StandardCharsets.UTF_8);
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK, buffer);
        response.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        response.headers().add(HttpHeaderNames.SERVER, Constants.SERVER_SW);
        response.headers().add(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON);
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(buffer.writerIndex()));
        return response;
    }

    private TextWebSocketFrame getWsResponse(String msg) {
        return new TextWebSocketFrame(msg);
    }
}
