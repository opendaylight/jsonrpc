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
 * {@link BusSessionFactory} implemented using secured websocket channel.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 9, 2018
 */
public class WssBusSessionFactory extends AbstractWebBusSessionFactory {
    public WssBusSessionFactory() {
        super("wss", true, true, 443);
    }

    public WssBusSessionFactory(EventLoopGroup bossGroup, EventLoopGroup workerGroup,
            EventExecutorGroup handlerExecutor) {
        super("wss", true, true, 443, bossGroup, workerGroup, handlerExecutor);
    }
}
