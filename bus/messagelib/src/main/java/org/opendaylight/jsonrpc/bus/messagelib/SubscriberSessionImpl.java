/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.api.Subscriber;

/**
 * Implementation of {@link SubscriberSession}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 24, 2018
 */
public class SubscriberSessionImpl extends AbstractSession implements SubscriberSession {
    private final Subscriber subscriber;

    public SubscriberSessionImpl(CloseCallback closeCallback, BusSessionFactory factory,
            NotificationMessageHandler handler, String topic, String uri) {
        super(closeCallback);
        subscriber = factory.subscriber(uri, topic, new NotificationHandlerAdapter(handler));
        setAutocloseable(subscriber);
    }

    @Override
    public void await() {
        subscriber.awaitConnection();
    }
}
