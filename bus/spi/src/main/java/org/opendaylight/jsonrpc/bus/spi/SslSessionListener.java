/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import javax.net.ssl.SSLSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inbound handler that listen for SSL handshake event and save negotiated session params into {@link Channel}'s
 * attribute {@link CommonConstants#ATTR_SSL_INFO} for later retrieval.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 18, 2019
 */
public class SslSessionListener extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(SslSessionListener.class);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (SslHandshakeCompletionEvent.SUCCESS.equals(evt)) {
            final SSLSession session = ctx.pipeline().get(SslHandler.class).engine().getSession();
            final SslSessionInfo sslInfo = new SslSessionInfo(session.getProtocol(), session.getCipherSuite());
            ctx.channel().attr(CommonConstants.ATTR_SSL_INFO).set(sslInfo);
            LOG.debug("Negotiated SSL params : {}, removing listener", sslInfo);
            // no longer needed to keep this handler in pipeline
            ctx.pipeline().remove(this);
        }
        super.userEventTriggered(ctx, evt);
    }
}
