/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.Map;

import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.spi.AbstractServerChannelInitializer;
import org.opendaylight.jsonrpc.bus.spi.ChannelGroupHandler;
import org.opendaylight.jsonrpc.bus.spi.CommonConstants;
import org.opendaylight.jsonrpc.bus.spi.SslSessionListener;
import org.opendaylight.jsonrpc.security.api.AuthenticationProvider;
import org.opendaylight.jsonrpc.security.api.SecurityConstants;
import org.opendaylight.jsonrpc.security.api.SslContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ChannelInitializer} for server-based endpoints (responder, publisher).
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 8, 2018
 */
class ServerInitializer extends AbstractServerChannelInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(ServerInitializer.class);
    private final boolean useSsl;
    private final Boolean isWebSocket;
    private final SslContext sslContext;
    private final Map<String, String> opts;
    private final AuthenticationProvider authenticationProvider;

    ServerInitializer(final SessionType socketType, final EventExecutorGroup handlerExecutor,
            final ChannelGroup channelGroup, final MessageListener messageListener, boolean useSsl,
            Map<String, String> opts, final boolean isWebSocket, final AuthenticationProvider authenticationProvider) {
        super(socketType, handlerExecutor, channelGroup, messageListener);
        this.opts = opts;
        this.useSsl = useSsl;
        this.isWebSocket = isWebSocket;
        this.authenticationProvider = authenticationProvider;
        sslContext = useSsl ? SslContextHelper.forServer(opts) : null;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        super.initChannel(ch);
        ch.attr(Constants.ATTR_URI_OPTIONS).set(opts);
        ch.attr(CommonConstants.ATTR_PEER_CONTEXT).set(new PeerContextImpl(ch, isWebSocket, useSsl));
        if (useSsl) {
            ch.pipeline().addLast(Constants.HANDLER_SSL, sslContext.newHandler(ch.alloc()));
            ch.pipeline().addLast(CommonConstants.HANDLER_SSL_INFO, new SslSessionListener());
        }
        configureLogging(ch);
        ch.pipeline().addLast(CommonConstants.HANDLER_CONN_TRACKER, new ChannelGroupHandler(channelGroup));
        ch.pipeline().addLast(CommonConstants.HANDLER_CODEC, new HttpServerCodec());
        ch.pipeline().addLast(Constants.HANDLER_AGGREGATOR, new HttpObjectAggregator(Constants.MESSAGE_SIZE));
        // setup authentication handler
        if (opts.containsKey(SecurityConstants.OPT_REQ_AUTH)) {
            LOG.debug("Authentication requested on channel {}, adding handler", ch);
            // don't invoke authentication handler on I/O loop, because it might involve blocking calls in AAA
            ch.pipeline().addLast(handlerExecutor, Constants.HANDLER_AUTH,
                    new ServerAuthHandler(authenticationProvider));
        }
        if (isWebSocket) {
            ch.pipeline().addLast(new WebSocketServerProtocolHandler("/", true));
            ch.pipeline().addLast(handlerExecutor, CommonConstants.HANDLER_LISTENER,
                    new WebSocketServerHandler(messageListener));
        } else {
            ch.pipeline().addLast(handlerExecutor, CommonConstants.HANDLER_LISTENER,
                    new HttpServerHandler(messageListener));
        }
    }
}
