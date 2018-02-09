/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import io.netty.buffer.ByteBuf;

/**
 * Default implementation of {@link ServerIndication}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 16, 2018
 */
public class DefaultServerIndication implements ServerIndication {
    private final boolean isServer;

    public DefaultServerIndication(ByteBuf msg) {
        Util.ensureEnoughData(32, msg);
        this.isServer = msg.readBoolean();
        msg.skipBytes(31);
    }

    public DefaultServerIndication(final boolean isServer) {
        this.isServer = isServer;
    }

    @Override
    public boolean isServer() {
        return isServer;
    }

    @Override
    public String toString() {
        return "DefaultServerIndication [isServer=" + isServer + "]";
    }
}
