/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.Objects;

import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.SessionType;

/**
 * Common code of server-based session type {@link ChannelInitializer}s.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 17, 2018
 */
@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class AbstractServerChannelInitializer extends AbstractChannelInitializer {
    protected final ChannelGroup channelGroup;
    protected final MessageListener messageListener;

    protected AbstractServerChannelInitializer(final SessionType socketType, final EventExecutorGroup handlerExecutor,
            final ChannelGroup channelGroup, final MessageListener messageListener) {
        super(socketType, handlerExecutor);
        this.channelGroup = Objects.requireNonNull(channelGroup);
        this.messageListener = Objects.requireNonNull(messageListener);
    }
}
