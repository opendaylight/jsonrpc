/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

public interface ClientSession extends BaseSession {

    /**
     * Flag to indicate connection readiness.
     *
     * @return true if connection is ready to send/receive messages, false otherwise
     */
    boolean isConnectionReady();

    /**
     * Block caller until this session is ready to talk to remote endpoint.
     */
    void await();
}
