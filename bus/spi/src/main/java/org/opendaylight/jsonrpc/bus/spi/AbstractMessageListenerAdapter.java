/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.ProgressivePromise;

import java.util.Objects;

import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.PeerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public abstract class AbstractMessageListenerAdapter<T> extends SimpleChannelInboundHandler<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMessageListenerAdapter.class);
    protected final MessageListener messageListener;

    protected AbstractMessageListenerAdapter(MessageListener messageListener) {
        this.messageListener = Objects.requireNonNull(messageListener);
    }

    protected void processResponse(ChannelHandlerContext ctx, final String bufferContent) {
        final ProgressivePromise<String> promise = ctx.channel()
                .attr(CommonConstants.ATTR_RESPONSE_QUEUE)
                .get()
                .getAndSet(null);
        if (promise != null) {
            promise.trySuccess(bufferContent);
        }
        final PeerContext peer = ctx.channel().attr(CommonConstants.ATTR_PEER_CONTEXT).get();
        messageListener.onMessage(peer, bufferContent);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error("Caught exception on {}, closing channel now", ctx.channel(), cause);
        super.exceptionCaught(ctx, cause);
        ctx.channel().close();
    }
}
