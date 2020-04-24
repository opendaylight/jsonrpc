/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpMessage;
import java.nio.charset.StandardCharsets;
import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.PeerContext;
import org.opendaylight.jsonrpc.bus.spi.AbstractMessageListenerAdapter;
import org.opendaylight.jsonrpc.bus.spi.CommonConstants;

/**
 * Final inbound message handler adapter.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 18, 2018
 */
public class HttpServerHandler extends AbstractMessageListenerAdapter<FullHttpMessage> {
    public HttpServerHandler(MessageListener messageListener) {
        super(messageListener);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpMessage msg) throws Exception {
        final PeerContext peer = ctx.channel().attr(CommonConstants.ATTR_PEER_CONTEXT).get();
        messageListener.onMessage(peer, msg.content().toString(StandardCharsets.UTF_8));
    }
}
