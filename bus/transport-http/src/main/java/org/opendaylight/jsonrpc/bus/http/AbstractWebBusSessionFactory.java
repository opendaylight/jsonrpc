/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import com.google.common.base.Splitter;
import com.google.common.base.Splitter.MapSplitter;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.Publisher;
import org.opendaylight.jsonrpc.bus.api.Requester;
import org.opendaylight.jsonrpc.bus.api.Responder;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.api.Subscriber;
import org.opendaylight.jsonrpc.bus.spi.AbstractBusSessionFactory;
import org.opendaylight.jsonrpc.bus.spi.DiscardingMessageListener;

/**
 * Base class of web-based {@link BusSessionFactory} implementations.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 9, 2018
 */
abstract class AbstractWebBusSessionFactory extends AbstractBusSessionFactory {
    private static final MapSplitter QUERY_SPLITER = Splitter.on('&').withKeyValueSeparator('=');
    protected final boolean useSsl;
    protected final boolean isWebsocket;
    protected final int defaultPort;

    AbstractWebBusSessionFactory(final String name, final boolean useSsl, final boolean isWebsocket, int defaultPort) {
        super(name);
        this.isWebsocket = isWebsocket;
        this.useSsl = useSsl;
        this.defaultPort = defaultPort;
    }

    AbstractWebBusSessionFactory(String name, final boolean useSsl, final boolean isWebsocket, int defaultPort,
            EventLoopGroup bossGroup, EventLoopGroup workerGroup, EventExecutorGroup handlerExecutor) {
        super(name, bossGroup, workerGroup, handlerExecutor);
        this.isWebsocket = isWebsocket;
        this.useSsl = useSsl;
        this.defaultPort = defaultPort;
    }

    @Override
    public Publisher publisher(String uri) {
        /*
         * PUB/SUB on HTTP does not make much sense.
         */
        if (!isWebsocket) {
            throwUnsupported(SessionType.PUB);
        }
        final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        final Publisher session = new PublisherImpl(uri, defaultPort, serverBootstrap,
                createServerInitializer(SessionType.PUB, DiscardingMessageListener.INSTANCE, channelGroup, uri),
                channelGroup);
        addSession(session);
        return session;
    }

    @Override
    public Subscriber subscriber(String uri, String topic, MessageListener listener) {
        if (!isWebsocket) {
            throwUnsupported(SessionType.SUB);
        }
        final Subscriber session = new SubscriberImpl(uri, defaultPort, clientBootstrap,
                createClientInitializer(SessionType.SUB, handlerExecutor, uri, listener));
        addSession(session);
        return session;
    }

    @Override
    public Requester requester(String uri, MessageListener listener) {
        final Requester session = new RequesterImpl(uri, defaultPort, clientBootstrap,
                createClientInitializer(SessionType.REQ, handlerExecutor, uri, listener), isWebsocket);
        addSession(session);
        return session;
    }

    @Override
    public Responder responder(String uri, MessageListener listener) {
        final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        final Responder session = new ResponderImpl(uri, defaultPort, serverBootstrap,
                createServerInitializer(SessionType.REP, listener, channelGroup, uri), channelGroup);
        addSession(session);
        return session;
    }

    private ChannelInitializer<SocketChannel> createServerInitializer(SessionType socketType, MessageListener listener,
            ChannelGroup channelGroup, String uri) {
        return new ServerInitializer(socketType, handlerExecutor, channelGroup, listener, useSsl, getOptions(uri),
                isWebsocket);
    }

    private Map<String, String> getOptions(String uriStr) {
        final URI uri = createUriUnchecked(uriStr);
        return Maps.newLinkedHashMap(
                !Strings.isNullOrEmpty(uri.getQuery()) ? QUERY_SPLITER.split(uri.getQuery()) : Collections.emptyMap());
    }

    private ChannelInitializer<SocketChannel> createClientInitializer(SessionType socketType,
            EventExecutorGroup handlerExecutor, String uri, MessageListener listener) {
        return new ClientInitializer(socketType, handlerExecutor, useSsl, isWebsocket, createUriUnchecked(uri),
                getOptions(uri), listener);
    }

    private void throwUnsupported(SessionType sessionType) {
        throw new UnsupportedOperationException(
                String.format("Transport '%s' does not support %s session type", name, sessionType.name()));
    }
}
