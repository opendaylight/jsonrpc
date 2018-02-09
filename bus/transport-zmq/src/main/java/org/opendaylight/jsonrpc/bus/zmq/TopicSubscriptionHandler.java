/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * {@link ChannelHandler} which send topic to subscriber after handshake is
 * completed.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 7, 2018
 */
public class TopicSubscriptionHandler extends ChannelInboundHandlerAdapter {
    private final String topic;

    public TopicSubscriptionHandler(String topic) {
        this.topic = topic;
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, Object evt) throws Exception {
        if (Constants.HANDSHAKE_COMPLETED.equals(evt)) {
            ctx.channel().writeAndFlush(new SubscribeMessage(topic)).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    ctx.pipeline().remove(TopicSubscriptionHandler.this);
                }
            });
        }
        super.userEventTriggered(ctx, evt);
    }
}
