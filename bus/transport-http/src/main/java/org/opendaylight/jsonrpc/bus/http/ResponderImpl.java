/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import org.opendaylight.jsonrpc.bus.api.Responder;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.spi.AbstractServerSession;

class ResponderImpl extends AbstractServerSession implements Responder {
    ResponderImpl(String uri, int defaultPort, ServerBootstrap serverBootstrap,
            ChannelInitializer<SocketChannel> channelInitializer, ChannelGroup channelGroup) {
        super(uri, defaultPort, channelGroup, SessionType.REP);
        channelFuture = serverBootstrap.childHandler(channelInitializer)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .bind(address)
                .syncUninterruptibly();

    }
}
