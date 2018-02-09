/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.SystemPropertyUtil;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holder for shared instance of {@link EventLoopGroup}, normally used in
 * embedded applications.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 22, 2018
 */
public final class EventLoopGroupProvider {
    private static final Logger LOG = LoggerFactory.getLogger(EventLoopGroupProvider.class);
    private static EventLoopGroup instance;

    /**
     * Get shared {@link EventLoopGroup}.
     *
     * @return shared {@link EventLoopGroup}
     */
    public static EventLoopGroup getSharedGroup() {
        // Lazy initialize
        synchronized (EventLoopGroupProvider.class) {
            if (instance == null) {
                final int size = SystemPropertyUtil.getInt("jsonrpc.eventloop.size", 8);
                instance = new NioEventLoopGroup(size);
                LOG.debug("Created shared eventloop group of size {}", size);
            }
            return instance;
        }
    }

    private EventLoopGroupProvider() {
        // prevent others to instantiate this class
    }

    /**
     * Shutdown everything.
     */
    public static void shutdown() {
        getSharedGroup().shutdownGracefully(0, 10, TimeUnit.MILLISECONDS);
    }
}