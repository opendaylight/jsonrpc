/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import java.util.Objects;
import org.opendaylight.jsonrpc.bus.api.SessionType;

/**
 * Base type of {@link ChannelInitializer}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 17, 2018
 */
@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public abstract class AbstractChannelInitializer extends ChannelInitializer<SocketChannel> {
    protected final SessionType socketType;
    protected final EventExecutorGroup handlerExecutor;

    public AbstractChannelInitializer(SessionType socketType, EventExecutorGroup handlerExecutor) {
        this.socketType = Objects.requireNonNull(socketType);
        this.handlerExecutor = Objects.requireNonNull(handlerExecutor);
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.attr(CommonConstants.ATTR_SOCKET_TYPE).set(socketType);
        ch.attr(CommonConstants.ATTR_HANDSHAKE_DONE).set(false);
    }

    protected static void configureLogging(Channel channel) {
        if (CommonConstants.DEBUG_MODE) {
            channel.pipeline().addLast(CommonConstants.HANDLER_LOGGING, CommonConstants.LOG_HANDLER);
        }
    }

    public EventExecutorGroup eventExecutor() {
        return handlerExecutor;
    }
}
