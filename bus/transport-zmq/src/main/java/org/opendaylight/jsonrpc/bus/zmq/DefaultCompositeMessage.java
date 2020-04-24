/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of {@link CompositeMessage}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 17, 2018
 */
public class DefaultCompositeMessage implements CompositeMessage {
    private final List<Message> messages = new ArrayList<>();

    public DefaultCompositeMessage(Message... msgs) {
        messages.addAll(Arrays.asList(msgs));
    }

    @Override
    public List<Message> messages() {
        return Collections.unmodifiableList(messages);
    }

    @Override
    public ByteBuf toBuffer() {
        final ByteBuf buffer = Unpooled.buffer();
        final int size = messages.size();
        if (size == 0) {
            return Unpooled.EMPTY_BUFFER;
        }
        if (size == 1) {
            write(true, messages.get(0), buffer);
            return buffer;
        } else {
            for (int i = 0; i < messages.size() - 1; i++) {
                write(false, messages.get(i), buffer);
            }
            write(true, messages.get(size - 1), buffer);
            return buffer;
        }
    }

    private void write(boolean last, Message message, ByteBuf buffer) {
        final ByteBuf content = message.toBuffer();
        if (content.readableBytes() > 255) {
            buffer.writeByte(last ? Constants.LAST_MESSAGE_LONG_SIZE : Constants.MESSAGE_LONG_SIZE);
            buffer.writeLong(content.readableBytes());
        } else {
            buffer.writeByte(last ? Constants.LAST_MESSAGE_SHORT_SIZE : Constants.MESSAGE_SHORT_SIZE);
            buffer.writeByte(content.readableBytes());
        }
        buffer.writeBytes(content);
    }

    @Override
    public String toString() {
        return "DefaultCompositeMessage [messages=" + messages + "]";
    }
}
