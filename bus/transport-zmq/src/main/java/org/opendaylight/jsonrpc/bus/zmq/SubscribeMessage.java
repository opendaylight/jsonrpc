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
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class SubscribeMessage implements Message {
    private final String topic;

    public SubscribeMessage(ByteBuf buffer) {
        Util.ensureEnoughData(3, buffer);
        buffer.skipBytes(1);
        final int len = buffer.readByte();
        Util.ensureEnoughData(len, buffer);
        buffer.skipBytes(1);
        topic = buffer.readCharSequence(len - 1, StandardCharsets.US_ASCII).toString();
    }

    public SubscribeMessage(final String topic) {
        this.topic = Objects.requireNonNull(topic);
    }

    @Override
    public ByteBuf toBuffer() {
        final ByteBuf content = Unpooled.buffer();
        content.writeByte(0x00);
        content.writeByte(topic.length() + 1);
        content.writeByte(0x01);
        content.writeCharSequence(topic, StandardCharsets.US_ASCII);
        return content;
    }

    public String topic() {
        return topic;
    }

    @Override
    public boolean last() {
        return true;
    }
}
