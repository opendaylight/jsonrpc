/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.spi.AbstractServerChannelInitializer;
import org.opendaylight.jsonrpc.bus.spi.ChannelGroupHandler;
import org.opendaylight.jsonrpc.bus.spi.CommonConstants;

/**
 * {@link ChannelInitializer} for server-based session types (publisher,
 * responder).
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 6, 2018
 */
public class ServerInitializer extends AbstractServerChannelInitializer {
    public ServerInitializer(ChannelGroup channelGroup, final MessageListener messageListener, SessionType socketType,
            EventExecutorGroup handlerExecutor) {
        super(socketType, handlerExecutor, channelGroup, messageListener);
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        super.initChannel(ch);
        ch.attr(CommonConstants.ATTR_PEER_CONTEXT).set(new PeerContextImpl(ch));
        configureLogging(ch);
        ch.pipeline().addLast(CommonConstants.HANDLER_CONN_TRACKER, new ChannelGroupHandler(channelGroup));
        ch.pipeline().addLast(Constants.HANDLER_HANDSHAKE, new HandshakeHandler());
        ch.pipeline().addLast(Constants.HANDLER_ENCODER, new MessageEncoder());
        ch.pipeline().addLast(Constants.HANDLER_DECODER, new MessageDecoder());
        ch.pipeline().addLast(handlerExecutor, CommonConstants.HANDLER_LISTENER, new ServerHandler(messageListener));
    }
}
