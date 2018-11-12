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
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

/**
 * Common code shared across multiple classes.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 23, 2018
 */
final class HttpUtil {
    private HttpUtil() {
        // no instantiation here
    }

    /**
     * Get transport name (protocol).
     *
     * @param isWebsocket flag to indicate websocket
     * @param isSsl flag to indicate use of SSL/TLS
     * @return transport name
     */
    public static String getTransport(boolean isWebsocket, boolean isSsl) {
        if (isWebsocket) {
            return isSsl ? "wss" : "ws";
        } else {
            return isSsl ? "https" : "http";
        }
    }

    public static Object createRequestObject(final boolean isWebSocket, final String message) {
        return isWebSocket ? createWebsocketFrame(message) : createHttpRequest(message);
    }

    private static HttpRequest createHttpRequest(final String msg) {
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeCharSequence(msg, StandardCharsets.UTF_8);
        final DefaultFullHttpRequest http = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/",
                buffer);
        /*
         * Consider adding some authentication mechanism.
         */
        http.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        http.headers().add(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON);
        http.headers().add(HttpHeaderNames.USER_AGENT, Constants.USER_AGENT);
        http.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        http.headers().add(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(buffer.readableBytes()));
        return http;
    }

    private static WebSocketFrame createWebsocketFrame(final String message) {
        return new TextWebSocketFrame(message);
    }

    public static URI stripPathAndQueryParams(URI inUri) {
        try {
            return new URI(inUri.getScheme(), null, inUri.getHost(), inUri.getPort(), null, null, null);
        } catch (URISyntaxException e) {
            // not possible to land here
            throw new IllegalStateException(e);
        }
    }
}
