/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import io.netty.channel.Channel;

import java.util.Objects;

import org.opendaylight.jsonrpc.bus.api.PeerContext;

/**
 * Common code for {@link PeerContext} implementations.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 12, 2018
 */
public abstract class AbstractPeerContext implements PeerContext {
    protected final Channel channel;
    protected final String transport;

    public AbstractPeerContext(final Channel channel, final String transport) {
        this.channel = Objects.requireNonNull(channel);
        this.transport = Objects.requireNonNull(transport);
    }

    @Override
    public Channel channel() {
        return channel;
    }

    public String transport() {
        return transport;
    }
}
