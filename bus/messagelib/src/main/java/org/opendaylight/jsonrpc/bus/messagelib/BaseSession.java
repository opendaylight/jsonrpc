/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

/**
 * Common interface of all session types.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 25, 2018
 */
public interface BaseSession extends AutoCloseable {
    @Override
    void close();

    /**
     * Set communication timeout for this session.
     *
     * @param timeoutMilliseconds timeout in milliseconds
     */
    void setTimeout(long timeoutMilliseconds);
}
