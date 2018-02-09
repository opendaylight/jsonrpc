/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

/**
 * Strategy to allow custom reconnection timeout.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 6, 2018
 */
public interface ReconnectStrategy {
    /**
     * Get timeout based on current state.
     */
    long timeout();

    /**
     * Reset internal state, normally called after successful connection.
     */
    void reset();
}