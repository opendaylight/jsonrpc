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
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.ProgressivePromise;

import java.nio.charset.StandardCharsets;

import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.spi.CommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client handler to invoke {@link MessageListener} on message reception.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 25, 2018
 */
class ClientHandler extends SimpleChannelInboundHandler<ProtocolObject> {
    private static final Logger LOG = LoggerFactory.getLogger(ClientHandler.class);
    private final MessageListener listener;
    private boolean first = true;

    ClientHandler(MessageListener listener) {
        this.listener = listener;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProtocolObject msg) throws Exception {
        LOG.debug("Message : {}", msg);
        final PeerContextImpl peer = (PeerContextImpl) ctx.channel().attr(CommonConstants.ATTR_PEER_CONTEXT).get();
        final ByteBuf buffer = msg.toBuffer();
        if (first && !buffer.isReadable()) {
            LOG.debug("First empty frame discarded : {}", msg);
        } else {
            checkLast((Message) msg);
            final String bufferContent = buffer.toString(StandardCharsets.UTF_8);
            final ProgressivePromise<String> promise = ctx.channel()
                    .attr(CommonConstants.ATTR_RESPONSE_QUEUE)
                    .get()
                    .getAndSet(null);
            if (promise != null) {
                promise.trySuccess(bufferContent);
            }
            listener.onMessage(peer, bufferContent);
        }
        buffer.release();
    }

    private void checkLast(Message msg) {
        if (msg.last()) {
            first = true;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error("Caught exception on {}, closing now", ctx.channel(), cause);
    }
}
