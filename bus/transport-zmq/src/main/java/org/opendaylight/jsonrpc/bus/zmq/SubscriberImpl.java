/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import io.netty.bootstrap.Bootstrap;
import io.netty.util.concurrent.EventExecutorGroup;
import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.api.Subscriber;
import org.opendaylight.jsonrpc.bus.spi.AbstractReconnectingClient;

/**
 * Implementation of {@link Subscriber} session type.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 7, 2018
 */
final class SubscriberImpl extends AbstractReconnectingClient implements Subscriber {

    SubscriberImpl(String uri, String topic, MessageListener listener, Bootstrap clientBootStrap,
            EventExecutorGroup handlerExecutor) {
        super(uri, 10000, clientBootStrap, new SubscriberInitializer(topic, listener, handlerExecutor),
                SessionType.SUB);
        connectInternal();
    }

    @Override
    public void close() {
        closeChannel();
        super.close();
    }
}
