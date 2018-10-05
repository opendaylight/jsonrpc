/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Configuration of various {@link EventLoopGroup}s.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 23, 2018
 */
public interface EventLoopConfiguration {
    /**
     * {@link EventLoopGroup} used to dispatch newly created {@link Channel}s.
     *
     * @return {@link EventLoopGroup}
     */
    EventLoopGroup bossGroup();

    /**
     * {@link EventLoopGroup} used to invoke {@link ChannelHandler} instances in {@link ChannelPipeline}.
     *
     * @return {@link EventLoopGroup}
     */
    EventLoopGroup workerGroup();

    /**
     * {@link EventExecutorGroup} used to execute (potentially blocking) code in last handler in
     * {@link ChannelPipeline}.
     *
     * @return {@link EventExecutorGroup}
     */
    EventExecutorGroup handlerGroup();
}