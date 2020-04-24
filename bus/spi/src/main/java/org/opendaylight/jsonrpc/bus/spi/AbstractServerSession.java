/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import io.netty.channel.group.ChannelGroup;
import java.util.Objects;
import org.opendaylight.jsonrpc.bus.api.ServerSession;
import org.opendaylight.jsonrpc.bus.api.SessionType;

/**
 * Server-based session type (publisher, responder).
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 7, 2018
 */
public abstract class AbstractServerSession extends AbstractSession implements ServerSession {
    protected final ChannelGroup channelGroup;

    public AbstractServerSession(String uri, int defaultPort, ChannelGroup channelGroup, SessionType sessionType) {
        super(uri, defaultPort, sessionType);
        this.channelGroup = Objects.requireNonNull(channelGroup);
    }

    @Override
    public void disconnectAll() {
        channelGroup.close();
    }

    @Override
    public void close() {
        disconnectAll();
        super.close();
    }
}
