/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.api;

/**
 * Base session type API.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 4, 2018
 */
public interface BusSession extends AutoCloseable {
    /**
     * Close this session. Call to this method should be idempotent and should
     * not throw exceptions.
     */
    @Override
    void close();

    /**
     * Get session type.
     *
     * @return {@link SessionType}
     */
    SessionType getSessionType();
}
