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
 * Protocol signature.
 * <p>
 * See <a href="https://rfc.zeromq.org/spec:23/ZMTP/">specification</a>.
 * </p>
 * ABNF grammar:
 *
 * <p>
 *
 * <pre>
 * signature = %xFF padding %x7F
 * padding = 8OCTET        ; Not significant
 * </pre>
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 16, 2018
 */
public interface Signature extends ProtocolObject {
    /**
     * Get padding value.
     *
     * @return padding value
     */
    long padding();

    default ByteBuf toBuffer() {
        return Unpooled.buffer()
                .writeByte(Constants.SIGNATURE_PREFIX)
                .writeLong(padding())
                .writeByte(Constants.SIGNATURE_SUFFIX);
    }
}
