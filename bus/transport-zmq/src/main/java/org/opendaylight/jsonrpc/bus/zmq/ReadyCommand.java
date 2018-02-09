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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.jsonrpc.bus.api.SessionType;

/**
 * Ready command.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 2, 2018
 */
public class ReadyCommand implements Command {
    private final String name;
    private final Map<String, ByteBuf> metadata = new HashMap<>();

    public ReadyCommand(ByteBuf buffer) {
        Util.ensureEnoughData(2, buffer);
        final byte flags = buffer.readByte();
        if ((flags & Constants.FLAG_COMMAND) == 0) {
            throw new IllegalArgumentException("Expected COMMAND FLAG");
        }
        final byte length = buffer.readByte();
        Util.ensureEnoughData(length, buffer);
        final byte readyLen = buffer.readByte();
        final String readyStr = buffer.readCharSequence(readyLen, StandardCharsets.US_ASCII).toString();
        if (!Constants.READY_STR.equals(readyStr)) {
            throw new IllegalArgumentException(String.format("Expected 'READY' string, but got '%s'", readyStr));
        }
        this.name = Constants.READY_STR;
        // read metadata
        int read = readyLen + 1;
        while (read < length) {
            byte keyLen = buffer.readByte();
            read += 1;
            final String mdKey = buffer.readCharSequence(keyLen, StandardCharsets.US_ASCII).toString();
            read += keyLen;
            final long valLen = buffer.readUnsignedInt();
            read += 4;
            if (valLen == 0) {
                metadata.put(mdKey, Unpooled.EMPTY_BUFFER);
            } else {
                final ByteBuf value = Unpooled.buffer((int) valLen);
                buffer.readBytes(value);
                read += valLen;
                metadata.put(mdKey, value);
            }
        }
    }

    public ReadyCommand(SessionType socketType) {
        this.name = Constants.READY_STR;
        metadata.put(Constants.METADATA_SOCKET_TYPE,
                Unpooled.buffer().writeBytes(socketType.name().getBytes(StandardCharsets.US_ASCII)));
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ByteBuf toBuffer() {
        final ByteBuf out = Unpooled.buffer();
        out.writeByte(name.length());
        out.writeCharSequence(name, StandardCharsets.US_ASCII);
        emitMetadata(out);
        final ByteBuf wrapper = Unpooled.buffer();
        wrapper.writeByte(Constants.FLAG_COMMAND);
        wrapper.writeByte(out.writerIndex());
        wrapper.writeBytes(out);
        return wrapper;
    }

    private void emitMetadata(ByteBuf out) {
        for (final Entry<String, ByteBuf> md : metadata.entrySet()) {
            out.writeByte(md.getKey().length());
            out.writeCharSequence(md.getKey(), StandardCharsets.US_ASCII);
            out.writeInt(md.getValue().readableBytes());
            out.writeBytes(md.getValue());
        }
    }

    /**
     * Get command metadata.
     *
     * @return read-only view of metadata.
     */
    public Map<String, ByteBuf> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    @Override
    public String toString() {
        return "ReadyCommand [name=" + name + ", metadata=" + metadata + "]";
    }
}
