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

import org.opendaylight.jsonrpc.bus.api.Publisher;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.spi.AbstractServerSession;

/**
 * Implementation of {@link Publisher} session.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 9, 2018
 */
class PublisherImpl extends AbstractServerSession implements Publisher {

    PublisherImpl(String uri, int defaultPort, ServerBootstrap serverBootstrap,
            ChannelInitializer<SocketChannel> channelInitializer, ChannelGroup channelGroup) {
        super(uri, defaultPort, channelGroup, SessionType.PUB);
        channelFuture = serverBootstrap.childHandler(channelInitializer)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .bind(address)
                .syncUninterruptibly();
    }

    @Override
    public void publish(String message, String topic) {
        channelGroup.writeAndFlush(HttpUtil.createRequestObject(true, message));
    }
}
