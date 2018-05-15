/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.ProgressivePromise;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.spi.AbstractChannelInitializer;
import org.opendaylight.jsonrpc.bus.spi.CommonConstants;

/**
 * Initializer of client based session types (requester,subscriber).
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 8, 2018
 */
class ClientInitializer extends AbstractChannelInitializer {
    private final boolean useSsl;
    private final SslContext sslContext;
    private final boolean isWebsocket;
    private final URI baseUri;
    private final MessageListener listener;
    private final AtomicReference<ProgressivePromise<String>> currentRequest = new AtomicReference<>(null);

    ClientInitializer(SessionType socketType, EventExecutorGroup handlerExecutor, boolean useSsl, boolean isWebsocket,
            URI baseUri, Map<String, String> opts, MessageListener listener) {
        super(socketType, handlerExecutor);
        this.useSsl = useSsl;
        this.isWebsocket = isWebsocket;
        this.baseUri = baseUri;
        this.listener = listener;
        if (useSsl) {
            try {
                final TrustManagerFactory trustManagerFactory = Constants.DEFAULT_CERT_POLICY
                        .equals(opts.computeIfAbsent(Constants.OPT_CERT_TRUST, v -> Constants.DEFAULT_CERT_POLICY))
                                ? InsecureTrustManagerFactory.INSTANCE : null;
                sslContext = SslContextBuilder.forClient().trustManager(trustManagerFactory).build();
            } catch (SSLException e) {
                throw new IllegalStateException("Failed to initialize SSL", e);
            }
        } else {
            sslContext = null;
        }
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        super.initChannel(ch);
        ch.attr(CommonConstants.ATTR_PEER_CONTEXT).set(new PeerContextImpl(ch, isWebsocket, useSsl));
        ch.attr(CommonConstants.ATTR_RESPONSE_QUEUE).set(currentRequest);
        if (useSsl) {
            ch.pipeline().addLast(Constants.HANDLER_SSL, sslContext.newHandler(ch.alloc()));
        }
        if (!isWebsocket) {
            // there is no handshake for HTTP
            ch.attr(CommonConstants.ATTR_HANDSHAKE_DONE).set(true);
        }
        ch.pipeline().addLast(CommonConstants.HANDLER_CODEC, new HttpClientCodec());
        ch.pipeline().addLast(Constants.HANDLER_AGGREGATOR, new HttpObjectAggregator(256 * 1024));
        if (isWebsocket) {
            final WebSocketClientHandshaker handshaker = getWsHandshaker();
            ch.pipeline().addLast(Constants.HANDLER_WS_HANDSHAKE, new WebSocketClientHandshake(handshaker));
        }
        configureLogging(ch);
        if (isWebsocket) {
            ch.pipeline().addLast(handlerExecutor, new WebSocketClientHandler(listener));
        } else {
            ch.pipeline().addLast(handlerExecutor, Constants.HANDLER_CLIENT, new HttpClientHandler(listener));
        }
    }

    private WebSocketClientHandshaker getWsHandshaker() {
        return WebSocketClientHandshakerFactory.newHandshaker(baseUri, WebSocketVersion.V13, null, true,
                new DefaultHttpHeaders());
    }
}
