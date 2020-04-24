/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import io.netty.util.AttributeKey;
import org.opendaylight.jsonrpc.bus.api.SessionType;

/**
 * Common constants.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 16, 2018
 */
public final class Constants {
    private static final BiMap<SessionType, Byte> ZMTP20_SOCKETS = ImmutableBiMap.<SessionType, Byte>builder()
            .put(SessionType.PUB, (byte) 0x01)
            .put(SessionType.SUB, (byte) 0x02)
            .put(SessionType.REQ, (byte) 0x03)
            .put(SessionType.REP, (byte) 0x04)
            .build();

    public static final int SIGNATURE_PREFIX = 0xff;
    public static final int SIGNATURE_SUFFIX = 0x7f;
    public static final byte CURRENT_MAJOR_VERSION = 3;
    public static final byte CURRENT_MINOR_VERSION = 0;
    public static final byte LAST_MESSAGE_SHORT_SIZE = 0x00;
    public static final byte MESSAGE_SHORT_SIZE = 0x01;
    public static final byte LAST_MESSAGE_LONG_SIZE = 0x02;
    public static final byte MESSAGE_LONG_SIZE = 0x03;
    public static final byte COMMAND_SHORT_SIZE = 0x04;
    public static final byte COMMAND_LONG_SIZE = 0x06;

    /**
     * Bit 2 (COMMAND): Command frame. A value of 1 indicates that the frame is
     * a command frame. A value of 0 indicates that the frame is a message
     * frame.
     */
    public static final byte FLAG_COMMAND = 0x04;

    /**
     * Bit 1 (LONG): Long frame. A value of 0 indicates that the frame size is
     * encoded as a single octet. A value of 1 indicates that the frame size is
     * encoded as a 64-bit unsigned integer in network byte order.
     */
    public static final byte FLAG_LONG_FRAME = 0x02;

    /**
     * Bit 0 (MORE): More frames to follow. A value of 0 indicates that there
     * are no more frames to follow. A value of 1 indicates that more frames
     * will follow. This bit SHALL be zero on command frames.
     */
    public static final byte FLAG_MORE = 0x01;

    /**
     * Ready command string.
     */
    public static final String READY_STR = "READY";

    /**
     * The Socket-Type announces the ZeroMQ socket type of the sending peer.
     */
    public static final String METADATA_SOCKET_TYPE = "Socket-Type";

    /**
     * A REQ, DEALER, or ROUTER peer connecting to a ROUTER MAY announce its
     * identity, which is used as an addressing mechanism by the ROUTER socket.
     * For all other socket types, the Identity property shall be ignored.
     */
    public static final String METADATA_IDENTITY = "Identity";
    public static final Signature DEFAULT_SINATURE = new DefaultSignature();
    public static final Mechanism NULL_AUTH = new DefaultMechanism("NULL");
    public static final AttributeKey<String> ATTR_PUBSUB_TOPIC = AttributeKey.valueOf(Constants.class, "PUBSUB_TOPIC");
    public static final String HANDLER_HANDSHAKE = "handshake";
    public static final String HANDLER_CLIENT = "client";
    public static final String HANDLER_ENCODER = "decoder";
    public static final String HANDLER_DECODER = "encoder";
    public static final String HANDLER_SUBSCRIBER_INITIALIZER = "topic-init";
    public static final String HANDSHAKE_COMPLETED = "HANDSHAKE_COMPLETED";
    public static final String TRANSPORT_NAME = "zmq";

    public static byte getZmtp20Socket(SessionType sessionType) {
        return ZMTP20_SOCKETS.get(sessionType);
    }

    public static SessionType getSessionType(byte value) {
        return ZMTP20_SOCKETS.inverse().get(value);
    }

    private Constants() {
        // no instantiation
    }
}
