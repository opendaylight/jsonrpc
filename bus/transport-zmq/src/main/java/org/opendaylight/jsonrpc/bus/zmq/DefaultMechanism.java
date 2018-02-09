/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.StringUtil;

/**
 * Default implementation of {@link Mechanism}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 17, 2018
 */
public class DefaultMechanism implements Mechanism {
    private final String name;

    public DefaultMechanism(String name) {
        if (StringUtil.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Name must not be empty");
        }
        this.name = name;
    }

    public DefaultMechanism(ByteBuf byteBuf) {
        Util.ensureEnoughData(20, byteBuf);
        final StringBuilder sb = new StringBuilder();
        byteBuf.forEachByte(byteBuf.readerIndex(), 20, b -> {
            if (b == 0) {
                return false;
            }
            sb.append((char) (b & 0xFF));
            return true;
        });
        byteBuf.skipBytes(20);
        name = sb.toString();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return "DefaultMechanism [name=" + name + "]";
    }
}
