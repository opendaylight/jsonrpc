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
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import org.opendaylight.jsonrpc.bus.spi.CommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Protocol message decoder.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 17, 2018
 */
public class MessageDecoder extends ByteToMessageDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(MessageDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (CommonConstants.DEBUG_MODE) {
            LOG.trace("IN {} : Decoding {}", ctx.channel(), ByteBufUtil.hexDump(in));
        }
        in.markReaderIndex();
        final byte start = in.readByte();
        switch (start) {
            case Constants.MESSAGE_SHORT_SIZE:
            case Constants.COMMAND_SHORT_SIZE:
            case Constants.LAST_MESSAGE_SHORT_SIZE: {
                final short len = in.readUnsignedByte();
                if (in.readableBytes() < len) {
                    in.resetReaderIndex();
                    return;
                } else {
                    out.add(readObject(in, len, start == Constants.COMMAND_SHORT_SIZE,
                            start == Constants.LAST_MESSAGE_SHORT_SIZE));
                }
            }
                break;

            case Constants.COMMAND_LONG_SIZE:
            case Constants.MESSAGE_LONG_SIZE:
            case Constants.LAST_MESSAGE_LONG_SIZE: {
                final long len = in.readLong();
                if (in.readableBytes() < len) {
                    in.resetReaderIndex();
                    return;
                } else {
                    out.add(readObject(in, (int) len, start == Constants.COMMAND_LONG_SIZE, false));
                }
            }
                break;

            default:
                break;
        }
    }

    private Object readObject(ByteBuf buffer, int len, boolean isCommand, boolean isLastMessage) {
        if (isCommand) {
            throw new UnsupportedOperationException("Not implemented");
        } else {
            return new DefaultMessage(isLastMessage, buffer.readBytes(len));
        }
    }
}
