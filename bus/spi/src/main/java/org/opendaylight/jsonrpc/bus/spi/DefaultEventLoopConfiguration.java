/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import java.util.Objects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Default implementation of {@link EventLoopConfiguration}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 23, 2018
 */
@Component(property = "name=jsonrpc")
public class DefaultEventLoopConfiguration implements EventLoopConfiguration {
    // event loop group used to dispatch newly accepted connection
    private final EventLoopGroup bossGroup;
    // event loop group used to invoke handlers in pipeline
    private final EventLoopGroup workerGroup;
    // event executor used to invoke final handler in pipeline, user code can use blocking
    private final EventExecutorGroup handlerGroup;

    @Activate
    public DefaultEventLoopConfiguration(
            @Reference(target = "(&(name=jsonrpc)(type=boss))") final EventLoopGroup bossGroup,
            @Reference(target = "(&(name=jsonrpc)(type=worker))") final EventLoopGroup workerGroup,
            @Reference(target = "(name=jsonrpc)") final EventExecutorGroup handlerGroup) {
        this.bossGroup = Objects.requireNonNull(bossGroup);
        this.workerGroup = Objects.requireNonNull(workerGroup);
        this.handlerGroup = Objects.requireNonNull(handlerGroup);
    }

    @Override
    public EventLoopGroup bossGroup() {
        return bossGroup;
    }

    @Override
    public EventLoopGroup workerGroup() {
        return workerGroup;
    }

    @Override
    public EventExecutorGroup handlerGroup() {
        return handlerGroup;
    }
}

