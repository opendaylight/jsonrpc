/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;

import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.api.Subscriber;

/**
 * {@link Subscriber} specific initializer.
 *
 * @see ClientInitializer
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 9, 2018
 */
public class SubscriberInitializer extends ClientInitializer {
    private final String topic;

    public SubscriberInitializer(String topic, MessageListener listener, EventExecutorGroup handlerExecutor) {
        super(SessionType.SUB, handlerExecutor, listener);
        this.topic = topic;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        super.initChannel(ch);
        ch.pipeline().addAfter(Constants.HANDLER_HANDSHAKE, Constants.HANDLER_SUBSCRIBER_INITIALIZER,
                new TopicSubscriptionHandler(topic != null ? topic : ""));
    }
}
