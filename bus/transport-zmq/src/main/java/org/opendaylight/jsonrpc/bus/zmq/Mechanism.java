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

/**
 * Security mechanism.
 *
 * <p>For details, check
 * <a href="https://rfc.zeromq.org/spec:23/ZMTP/">specification</a>.
 *
 * <p>ABNF grammar:
 *
 * <p><pre>
 * ;   The mechanism is a null padded string
 * mechanism = 20mechanism-char
 * mechanism-char = "A"-"Z" | DIGIT
 *     | "-" | "_" | "." | "+" | %x0
 * </pre>
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 17, 2018
 */
public interface Mechanism extends ProtocolObject {
    /**
     * Name of this mechanism.
     */
    String name();

    @Override
    default ByteBuf toBuffer() {
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeCharSequence(name(), StandardCharsets.US_ASCII);
        buffer.writeZero(20 - buffer.readableBytes());
        return buffer;
    }
}
