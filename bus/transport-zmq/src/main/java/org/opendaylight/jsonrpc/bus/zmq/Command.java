/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

/**
 * Command.
 *
 * <p>See <a href="https://rfc.zeromq.org/spec:23/ZMTP/">specification</a>.
 * ABNF grammar:
 * <pre>
 * ;   A command is a single long or short frame
 * command = command-size command-body
 * command-size = %x04 short-size | %x06 long-size
 * short-size = OCTET          ; Body is 0 to 255 octets
 * long-size = 8OCTET          ; Body is 0 to 2^63-1 octets
 * command-body = command-name command-data
 * command-name = OCTET 1*255command-name-char
 * command-name-char = ALPHA
 * command-data = *OCTET *
 * </pre>
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 16, 2018
 */
public interface Command extends ProtocolObject {
    /**
     * Command name.
     */
    String name();
}
