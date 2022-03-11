/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import io.netty.bootstrap.Bootstrap;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.spi.AbstractChannelInitializer;
import org.opendaylight.jsonrpc.bus.spi.AbstractReconnectingClient;

/**
 * Common code for client-like session types (requester, subscriber).
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 8, 2018
 */
abstract class AbstractClientSession extends AbstractReconnectingClient {
    protected final boolean isWebsocket;

    AbstractClientSession(String uri, int defaultPort, Bootstrap clientBootstrap,
            AbstractChannelInitializer channelInitializer, boolean isWebsocket, SessionType sessionType) {
        super(uri, defaultPort, clientBootstrap, channelInitializer, sessionType);
        this.isWebsocket = isWebsocket;
    }
}
