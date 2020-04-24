/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import java.nio.charset.StandardCharsets;
import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.spi.AbstractMessageListenerAdapter;
import org.opendaylight.jsonrpc.bus.spi.CommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Final inbound message handler adapter.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 18, 2018
 */
public class HttpClientHandler extends AbstractMessageListenerAdapter<FullHttpResponse> {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientHandler.class);

    protected HttpClientHandler(MessageListener messageListener) {
        super(messageListener);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        final String bufferContent = msg.content().toString(StandardCharsets.UTF_8);
        if (CommonConstants.DEBUG_MODE) {
            LOG.debug("Received HTTP response {} with content {}", msg.status().code(), bufferContent);
        }
        processResponse(ctx, bufferContent);
    }
}
