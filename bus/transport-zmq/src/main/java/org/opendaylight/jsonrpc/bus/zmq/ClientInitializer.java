/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.concurrent.atomic.AtomicReference;

import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.spi.AbstractChannelInitializer;
import org.opendaylight.jsonrpc.bus.spi.CommonConstants;

/**
 * {@link ChannelInitializer} for client-based session types (subscriber,
 * requester).
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 7, 2018
 */
class ClientInitializer extends AbstractChannelInitializer {
    private final MessageListener listener;

    ClientInitializer(final SessionType socketType, final EventExecutorGroup handlerExecutor,
            final MessageListener listener) {
        super(socketType, handlerExecutor);
        this.listener = listener;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        super.initChannel(ch);
        ch.attr(CommonConstants.ATTR_RESPONSE_QUEUE).set(new AtomicReference<>(null));
        ch.attr(CommonConstants.ATTR_PEER_CONTEXT).set(new PeerContextImpl(ch));
        configureLogging(ch);
        ch.pipeline().addLast(Constants.HANDLER_HANDSHAKE, new HandshakeHandler());
        ch.pipeline().addLast(Constants.HANDLER_DECODER, new MessageDecoder());
        ch.pipeline().addLast(Constants.HANDLER_ENCODER, new MessageEncoder());
        ch.pipeline().addLast(handlerExecutor, Constants.HANDLER_CLIENT, new ClientHandler(listener));
    }
}
