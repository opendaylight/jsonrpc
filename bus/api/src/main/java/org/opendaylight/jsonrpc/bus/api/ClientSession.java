/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.api;

/**
 * Client-like session (applicable to {@link SessionType#REQ} and
 * {@link SessionType#SUB}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 20, 2018
 */
public interface ClientSession extends BusSession {
    /**
     * Block until this endpoint is connected.
     */
    void awaitConnection();

    /**
     * Flag to indicate connection readiness.
     *
     * @return true if connection is read, false otherwise
     */
    boolean isReady();
}
