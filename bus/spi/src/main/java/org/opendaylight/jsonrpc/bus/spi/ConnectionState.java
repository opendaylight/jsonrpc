/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

/**
 * State of connection.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 6, 2018
 */
public enum ConnectionState {
    /**
     * No connection attempt yet.
     */
    INITIAL,

    /**
     * Connection attempt in progress.
     */
    CONNECTING,

    /**
     * Connection established.
     */
    CONNECTED,

    /**
     * Disconnected, no attempt to connect in future.
     */
    DONE;
}
