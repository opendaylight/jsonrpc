/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.StandardCharsets;

import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.spi.AbstractMessageListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server handler to invoke {@link MessageListener} on message reception. This
 * handler also take care of configuring topic for subscriber session type.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 6, 2018
 */
public class ServerHandler extends AbstractMessageListenerAdapter<Message> {
    private static final Logger LOG = LoggerFactory.getLogger(ServerHandler.class);
    private boolean first = true;

    public ServerHandler(final MessageListener messageListener) {
        super(messageListener);
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final Message msg) throws Exception {
        final PeerContextImpl peer = ctx.channel().attr(Constants.ATTR_REMOTE_PEER).get();
        if (peer.getSocketType() == SessionType.SUB) {
            // Subscription message
            final ByteBuf data = msg.toBuffer();
            if (data.readableBytes() > 1) {
                data.skipBytes(1);
                final String topic = data.readCharSequence(data.readableBytes(), StandardCharsets.US_ASCII).toString();
                LOG.info("Subscribing to topic '{}'", topic);
                ctx.channel().attr(Constants.ATTR_PUBSUB_TOPIC).set(topic);
            }
            checkLast(msg);
            return;
        }
        if (peer.getSocketType() == SessionType.REQ && first && msg.toBuffer().readableBytes() == 0) {
            LOG.debug("First empty frame discarded : {}", msg);
        } else {
            checkLast(msg);
            messageListener.onMessage(peer, msg.toBuffer().toString(StandardCharsets.UTF_8));
        }
    }

    private void checkLast(Message msg) {
        if (msg.last()) {
            first = true;
        }
    }
}
