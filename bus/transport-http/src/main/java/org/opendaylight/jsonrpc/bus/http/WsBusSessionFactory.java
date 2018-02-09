/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;

/**
 * {@link BusSessionFactory} implemented using plain websocket channel.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 9, 2018
 */
public class WsBusSessionFactory extends AbstractWebBusSessionFactory {
    public WsBusSessionFactory() {
        super("ws", false, true, 80);
    }

    public WsBusSessionFactory(EventLoopGroup bossGroup, EventLoopGroup workerGroup,
            EventExecutorGroup handlerExecutor) {
        super("ws", false, true, 80, bossGroup, workerGroup, handlerExecutor);
    }
}
