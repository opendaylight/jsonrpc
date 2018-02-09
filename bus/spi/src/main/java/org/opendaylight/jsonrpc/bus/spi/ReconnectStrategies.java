/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

/**
 * Factory for various {@link ReconnectStrategy}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 6, 2018
 */
public final class ReconnectStrategies {
    private ReconnectStrategies() {
        // no instantiation here
    }

    /**
     * {@link ReconnectStrategy} with fixed timeout (connection attempt will
     * always be in same interval).
     *
     * @param timeout timeout in milliseconds
     */
    public static ReconnectStrategy fixedStartegy(long timeout) {
        return new FixedReconnectStrategy(timeout);
    }

    private static final class FixedReconnectStrategy implements ReconnectStrategy {
        final long timeoutMilliseconds;

        private FixedReconnectStrategy(long timeout) {
            this.timeoutMilliseconds = timeout;
        }

        @Override
        public long timeout() {
            return timeoutMilliseconds;
        }

        @Override
        public void reset() {
            // no-op
        }
    }
}