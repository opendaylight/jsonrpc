/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

/**
 * Protocol message.
 *
 * <p>See <a href="https://rfc.zeromq.org/spec:23/ZMTP/">specification</a>.
 * ABNF grammar:
 *
 * <pre>
 * ;   A message is one or more frames
 * message = *message-more message-last
 * message-more = ( %x01 short-size | %x03 long-size ) message-body
 * message-last = ( %x00 short-size | %x02 long-size ) message-body
 * message-body = *OCTET
 * </pre>
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 17, 2018
 */
public interface Message extends ProtocolObject {
    /**
     * True is and only if this message is last.
     */
    boolean last();
}
