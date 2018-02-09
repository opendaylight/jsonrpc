/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.api;

/**
 * Server-like session types are bound to local socket and can have multiple
 * clients connected to them.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 4, 2018
 */
public interface ServerSession extends BusSession {
    /**
     * Disconnects all connected clients.
     */
    void disconnectAll();
}
