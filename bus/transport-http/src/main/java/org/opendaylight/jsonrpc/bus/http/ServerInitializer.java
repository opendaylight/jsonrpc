/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import com.google.common.base.Splitter;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.EventExecutorGroup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;

import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.spi.AbstractServerChannelInitializer;
import org.opendaylight.jsonrpc.bus.spi.ChannelGroupHandler;
import org.opendaylight.jsonrpc.bus.spi.CommonConstants;

/**
 * {@link ChannelInitializer} for server-based endpoints (responder, publisher).
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 8, 2018
 */
class ServerInitializer extends AbstractServerChannelInitializer {
    private final boolean useSsl;
    private final Boolean isWebSocket;
    private final SslContext sslContext;
    private static final Splitter COMMA_SPLITTER = Splitter.on(',');

    ServerInitializer(final SessionType socketType, final EventExecutorGroup handlerExecutor,
            final ChannelGroup channelGroup, final MessageListener messageListener, boolean useSsl,
            Map<String, String> opts, final boolean isWebSocket) {
        super(socketType, handlerExecutor, channelGroup, messageListener);
        this.useSsl = useSsl;
        this.isWebSocket = isWebSocket;
        sslContext = useSsl ? setupSsl(opts) : null;
    }

    private SslContext setupSsl(Map<String, String> opts) {
        try {
            final File certFile = new File(opts.get(Constants.OPT_CERT_FILE));
            final String password = opts.get(Constants.OPT_PRIVATE_KEY_PASSWORD);
            final Iterable<String> ciphers = opts.containsKey(Constants.OPT_CIPHERS)
                    ? COMMA_SPLITTER.split(opts.get(Constants.OPT_CIPHERS)) : null;
            final KeyStore ks = KeyStore
                    .getInstance(opts.computeIfAbsent(Constants.OPT_KEYSTORE, k -> Constants.DEFAULT_KEYSTORE));
            try (InputStream is = Files.newInputStream(certFile.toPath())) {
                ks.load(is, password.toCharArray());
            }
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(opts
                    .computeIfAbsent(Constants.OPT_KEY_MANAGER_FACTORY, v -> Constants.DEFAULT_KEY_MANAGER_FACTORY));
            kmf.init(ks, password.toCharArray());
            return SslContextBuilder.forServer(kmf).ciphers(ciphers).build();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Failed to initialize SSL", e);
        }
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        super.initChannel(ch);
        ch.attr(CommonConstants.ATTR_PEER_CONTEXT).set(new PeerContextImpl(ch, isWebSocket));
        if (useSsl) {
            ch.pipeline().addLast(Constants.HANDLER_SSL, sslContext.newHandler(ch.alloc()));
        }
        configureLogging(ch);
        ch.pipeline().addLast(CommonConstants.HANDLER_CONN_TRACKER, new ChannelGroupHandler(channelGroup));
        ch.pipeline().addLast(CommonConstants.HANDLER_CODEC, new HttpServerCodec());
        ch.pipeline().addLast(Constants.HANDLER_AGGREGATOR, new HttpObjectAggregator(Constants.MESSAGE_SIZE));
        if (isWebSocket) {
            ch.pipeline().addLast(new WebSocketServerProtocolHandler("/"));
            ch.pipeline().addLast(handlerExecutor, CommonConstants.HANDLER_LISTENER,
                    new WebSocketServerHandler(messageListener));
        } else {
            ch.pipeline().addLast(handlerExecutor, CommonConstants.HANDLER_LISTENER,
                    new HttpServerHandler(messageListener));
        }
    }
}
