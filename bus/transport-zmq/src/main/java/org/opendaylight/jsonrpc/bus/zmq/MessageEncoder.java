/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.opendaylight.jsonrpc.bus.spi.CommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encodes {@link ProtocolObject} into {@link ByteBuf}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 25, 2018
 */
public class MessageEncoder extends MessageToByteEncoder<ProtocolObject> {
    private static final Logger LOG = LoggerFactory.getLogger(MessageEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, ProtocolObject msg, ByteBuf out) throws Exception {
        final ByteBuf buffer = msg.toBuffer();
        if (CommonConstants.DEBUG_MODE) {
            LOG.debug("OUT {} : Encoded {} into {}", ctx.channel(), msg, ByteBufUtil.hexDump(buffer));
        }
        out.writeBytes(buffer);
    }
}
