/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

/**
 * State of handshake on current channel.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 16, 2018
 */
public enum HandshakeState {
    /**
     * Nothing has been done yet.
     */
    INITIAL,

    /**
     * Sent signature and version.
     */
    SIGNATURE,

    /**
     * ZMTP 3.0 major version.
     */
    VERSION_MAJOR,

    /**
     * ZMTP 3.0 minor version.
     */
    VERSION_MINOR,

    /**
     * ZMTP 2.0 socket type.
     */
    SOCKET_TYPE,

    /**
     * ZMTP 2.0 identity.
     */
    IDENTITY,

    /**
     * Sent mechanism, "as-server" and "filler".
     */
    MECHANISM,

    /**
     * Sent READY command (Socket-type + identity).
     */
    READY,

    /**
     * Handshake is done.
     */
    DONE
}
