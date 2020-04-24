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
import org.opendaylight.jsonrpc.bus.api.Publisher;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.spi.AbstractServerSession;
import org.opendaylight.jsonrpc.bus.spi.DiscardingMessageListener;

/**
 * Implementation of {@link Publisher} session.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 7, 2018
 */
class PublisherImpl extends AbstractServerSession implements Publisher {
    PublisherImpl(String uri, ServerBootstrap serverBootstrap, ChannelGroup channelGroup,
            EventExecutorGroup handlerExecutor) {
        super(uri, 10000, channelGroup, SessionType.PUB);
        channelFuture = serverBootstrap
                .childHandler(new ServerInitializer(channelGroup, DiscardingMessageListener.INSTANCE, SessionType.PUB,
                        handlerExecutor))
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .bind(address)
                .syncUninterruptibly();

    }

    @Override
    public void publish(String message, String topic) {
        channelGroup.writeAndFlush(Util.serializeMessage(message), new TopicChannelMatcher(topic));
    }
}
