/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import org.opendaylight.jsonrpc.bus.api.SessionType;

/**
 * Common helper methods.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 27, 2018
 */
final class Util {
    /**
     * Mapping for allowed socket types.
     *
     * <p>
     * See "The Socket-Type Property" in
     * <a href="https://rfc.zeromq.org/spec:23/ZMTP/">specification</a> for
     * details.
     */
    private static final Multimap<SessionType, SessionType> MATRIX_SOCKET = ArrayListMultimap.create();

    static {
        MATRIX_SOCKET.put(SessionType.REP, SessionType.REQ);
        MATRIX_SOCKET.put(SessionType.REQ, SessionType.REP);
        MATRIX_SOCKET.put(SessionType.PUB, SessionType.SUB);
        MATRIX_SOCKET.put(SessionType.SUB, SessionType.PUB);
    }

    private Util() {
        // no instantiation of this class
    }

    /**
     * Ensure that given {@link ByteBuf} has enough readable data, throw
     * exception otherwise.
     *
     * @param expected number of readable bytes in buffer
     * @param buffer {@link ByteBuf} to check
     */
    public static void ensureEnoughData(int expected, ByteBuf buffer) {
        final int actual = buffer.readableBytes();
        if (actual < expected) {
            throw new IllegalArgumentException(
                    String.format("Not enough data in buffer. Expected at least %d, but got %d", expected, actual));
        }
    }

    /**
     * Verify that announced remote socket type conforms to specification with
     * regards to local socket type.
     *
     * @param thisSocket local {@link SessionType}
     * @param remoteSocket remote {@link SessionType}
     * @return true if and only if remote {@link SessionType} is allowed to
     *         'talk' to local
     */
    public static boolean assertSocketType(SessionType thisSocket, SessionType remoteSocket) {
        return MATRIX_SOCKET.get(thisSocket).contains(remoteSocket);
    }

    /**
     * Serialize text message to protocol format.
     *
     * @param message text to send
     */
    public static CompositeMessage serializeMessage(String message) {
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeCharSequence(message, StandardCharsets.UTF_8);
        return new DefaultCompositeMessage(new DefaultMessage(false, Unpooled.EMPTY_BUFFER),
                new DefaultMessage(true, buffer));
    }
}
