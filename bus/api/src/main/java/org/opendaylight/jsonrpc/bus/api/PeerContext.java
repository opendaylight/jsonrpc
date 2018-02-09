/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.api;

import io.netty.channel.Channel;

/**
 * Transport specific context of remote end.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 11, 2018
 */
public interface PeerContext {
    /**
     * Get {@link Channel} associated with remote peer.
     *
     * @return {@link Channel}
     */
    Channel channel();

    /**
     * Send message down the channel.
     *
     * @param message message to send
     */
    void send(String message);
}
