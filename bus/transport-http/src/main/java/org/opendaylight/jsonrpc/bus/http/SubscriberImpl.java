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
import org.opendaylight.jsonrpc.bus.api.Subscriber;
import org.opendaylight.jsonrpc.bus.spi.AbstractChannelInitializer;
import org.opendaylight.jsonrpc.bus.spi.AbstractReconnectingClient;

/**
 * Implementation of {@link Subscriber} session.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 9, 2018
 */
class SubscriberImpl extends AbstractReconnectingClient implements Subscriber {
    SubscriberImpl(String uri, int defaultPort, Bootstrap clientBootstrap,
            AbstractChannelInitializer channelInitializer) {
        super(uri, defaultPort, clientBootstrap, channelInitializer, SessionType.SUB);
        connectInternal();
    }
}
