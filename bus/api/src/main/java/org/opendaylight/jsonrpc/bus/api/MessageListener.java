/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.api;

/**
 * Callback to be invoked on message reception.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 11, 2018
 */
public interface MessageListener {
    /**
     * This method is invoked once message arrives.
     *
     * @param peerContext transport specific remote peer context
     * @param message received message
     */
    void onMessage(PeerContext peerContext, String message);
}
