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
 * Combination of "as-server" and "filler" fields.
 *
 * <p>For details, check
 * <a href="https://rfc.zeromq.org/spec:23/ZMTP/">specification</a>.
 *
 * <p>ABNF grammar:
 *
 * <p><pre>
 * ;   Is the peer acting as server?
 * as-server = %x00 | %x01
 *
 * ;   The filler extends the greeting to 64 octets
 * filler = 31%x00             ; 31 zero octets
 * </pre>
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 16, 2018
 */
public interface ServerIndication extends ProtocolObject {
    /**
     * Flag that indicate if peer is acting as server.
     */
    boolean isServer();

    @Override
    default ByteBuf toBuffer() {
        return Unpooled.buffer(32).writeByte(isServer() ? 1 : 0).writeZero(31);
    }
}
