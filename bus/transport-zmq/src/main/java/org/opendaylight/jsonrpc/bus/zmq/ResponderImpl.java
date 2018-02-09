/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.Responder;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.spi.AbstractServerSession;

/**
 * Implementation of {@link Responder} session.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 6, 2018
 */
class ResponderImpl extends AbstractServerSession implements Responder {

    ResponderImpl(String uri, ServerBootstrap serverBootstrap, MessageListener listener, ChannelGroup channelGroup,
            EventExecutorGroup handlerExecutor) {
        super(uri, 10000, channelGroup, SessionType.REP);
        channelFuture = serverBootstrap
                .childHandler(new ServerInitializer(channelGroup, listener, SessionType.REP, handlerExecutor))
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .bind(address)
                .syncUninterruptibly();
    }
}
