/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.nio.charset.StandardCharsets;
import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.spi.AbstractMessageListenerAdapter;
import org.opendaylight.jsonrpc.bus.spi.CommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Last handler in client pipeline which dispatch received
 * {@link TextWebSocketFrame} to {@link MessageListener}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 23, 2018
 */
class WebSocketClientHandler extends AbstractMessageListenerAdapter<TextWebSocketFrame> {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketClientHandler.class);

    WebSocketClientHandler(final MessageListener messageListener) {
        super(messageListener);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        final String bufferContent = msg.content().toString(StandardCharsets.UTF_8);
        if (CommonConstants.DEBUG_MODE) {
            LOG.debug("Received websocket frame with content '{}'", bufferContent);
        }
        processResponse(ctx, bufferContent);
    }
}
