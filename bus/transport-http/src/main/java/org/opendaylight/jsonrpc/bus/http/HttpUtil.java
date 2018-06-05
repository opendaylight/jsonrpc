/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.opendaylight.jsonrpc.bus.spi.ChannelAuthentication;
import org.opendaylight.jsonrpc.security.api.SecurityConstants;

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

    public static Object createPayload(ChannelAuthentication auth, final boolean isWebSocket,
            final String message) {
        return isWebSocket ? createWebsocketFrame(message) : createHttpRequest(auth, message);
    }

    /**
     * Create HTTP401 response.
     *
     * @return {@link HttpResponse} with status code set to 401
     */
    public static HttpResponse createForbidenResponse() {
        final ByteBuf content = Unpooled.buffer();
        content.writeCharSequence("Connection not authenticated", StandardCharsets.UTF_8);
        final DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.UNAUTHORIZED, content);
        resp.headers().add(HttpHeaderNames.SERVER, Constants.SERVER_SW);
        resp.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        resp.headers().add(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(content.readableBytes()));
        return resp;
    }

    private static HttpRequest createHttpRequest(ChannelAuthentication auth, final String msg) {
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeCharSequence(msg, StandardCharsets.UTF_8);
        final DefaultFullHttpRequest http = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/",
                buffer);
        if (auth.isEnabled()) {
            http.headers().add(HttpHeaderNames.AUTHORIZATION, createAuthHeader(auth.getUsername(), auth.getPassword()));
        }
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

    /**
     * Make sure that specified key is present in {@link Map}. It is NOT
     * required that key has non-null mapping.
     *
     * @param options {@link Map} of key-value pairs
     * @param key key
     * @return mapped value (possible null)
     * @throws IllegalStateException if there is no mapping using provided key
     */
    public static String ensureOption(final Map<String, String> options, final String key) {
        Preconditions.checkState(options.containsKey(key), "Missing required key '%s', existing keys : '%s'", key,
                options.keySet());
        return options.get(key);
    }

    /**
     * Compute value of 'Authorization' header for basic HTTP authentication. This method will ensure that client
     * provided both username and password and URI options.
     *
     * @param options {@link Map} of key-value pairs
     * @return basic HTTP authentication header value
     */
    public static String createAuthHeader(final Map<String, String> options) {
        final String username = ensureOption(options, SecurityConstants.OPT_USERNAME);
        final String password = ensureOption(options, SecurityConstants.OPT_PASSWORD);
        return createAuthHeader(username, password);
    }

    private static String createAuthHeader(String username, String password) {
        return new StringBuilder().append("Basic ")
                .append(Base64.getEncoder()
                        .encodeToString((username + ':' + password).getBytes(StandardCharsets.UTF_8)))
                .toString();
    }

    /**
     * Parse basic authentication header value as an array of strings where
     * first element is username and next is password.
     *
     * @param headerValue HTTP basic authentication header value.
     * @return username and password
     */
    public static String[] parseBasicAuthHeader(final String headerValue) {
        final String[] parts = headerValue.split("\\s+");
        return new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8).split(":");
    }
}
