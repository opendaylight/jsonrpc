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

/**
 * Arbitrary byte value used in protocol negotiation.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Apr 18, 2018
 */
public class ByteWrapper implements ProtocolObject {
    private final byte value;

    public ByteWrapper(final byte value) {
        this.value = value;
    }

    @Override
    public ByteBuf toBuffer() {
        return Unpooled.buffer(1).writeByte(value);
    }

    @Override
    public String toString() {
        return "ByteWrapper [value=" + value + "]";
    }
}
