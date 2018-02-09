/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.api;

import io.netty.util.concurrent.Future;

/**
 * Requester session type allow to send requests to remote {@link Responder}
 * peer.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 4, 2018
 */
public interface Requester extends ClientSession {
    /**
     * Send request to the peer. Despite returning {@link Future}, this method
     * actually blocks until connection is established and (if required)
     * handshake is performed. Blocking behavior comes from original API design.
     *
     * @param message message to send.
     * @return {@link Future} containing the result of send. The Future blocks
     *         until the response is received or timeout expires, whichever
     *         comes first.
     */
    Future<String> send(String message);
}
