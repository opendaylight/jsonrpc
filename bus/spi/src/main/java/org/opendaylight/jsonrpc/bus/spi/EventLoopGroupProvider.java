/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.SystemPropertyUtil;
import java.util.concurrent.TimeUnit;

/**
 * Holder for shared instance of {@link EventLoopGroup}, normally used in embedded applications.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 22, 2018
 */
public final class EventLoopGroupProvider {
    private static final EventLoopGroup SHARED_GROUP;
    private static final EventExecutorGroup HANDLER_GROUP;
    private static final EventLoopConfiguration CONFIG;

    static {
        SHARED_GROUP = new MultiThreadIoEventLoopGroup(12, NioIoHandler.newFactory());
        HANDLER_GROUP = new DefaultEventExecutorGroup(SystemPropertyUtil.getInt("jsonrpc.eventloop.size", 12));
        CONFIG = new DefaultEventLoopConfiguration(SHARED_GROUP, SHARED_GROUP, HANDLER_GROUP);
    }

    private EventLoopGroupProvider() {
        // prevent others to instantiate this class
    }

    /**
     * Get shared {@link EventLoopGroup}.
     *
     * @return shared {@link EventLoopGroup}
     */
    public static EventLoopGroup getSharedGroup() {
        return SHARED_GROUP;
    }

    /**
     * Get {@link EventExecutorGroup} used by handlers at tail of Netty's pipeline.
     *
     * @return handlers' {@link EventLoopGroup}
     */
    public static EventExecutorGroup getHandlerGroup() {
        return HANDLER_GROUP;
    }

    /**
     * Get configuration.
     *
     * @return {@link EventLoopConfiguration}
     */
    public static EventLoopConfiguration config() {
        return CONFIG;
    }

    /**
     * Shutdown everything.
     */
    public static void shutdown() {
        final long awaitTermination = SystemPropertyUtil.getLong("jsonrpc.eventloop.await.termination", 500);
        SHARED_GROUP.shutdownGracefully(awaitTermination, awaitTermination, TimeUnit.MILLISECONDS);
        HANDLER_GROUP.shutdownGracefully(awaitTermination, awaitTermination, TimeUnit.MILLISECONDS);
        // block until graceful shutdown completes
        for (;;) {
            if (SHARED_GROUP.isShutdown() && HANDLER_GROUP.isShutdown()) {
                return;
            }
            Thread.yield();
            Uninterruptibles.sleepUninterruptibly(100L, TimeUnit.MILLISECONDS);
        }
    }
}
