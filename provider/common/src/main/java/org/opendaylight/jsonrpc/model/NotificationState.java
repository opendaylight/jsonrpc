/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import com.google.common.base.Preconditions;
import java.net.URISyntaxException;
import org.opendaylight.jsonrpc.bus.messagelib.NotificationMessageHandler;
import org.opendaylight.jsonrpc.bus.messagelib.SubscriberSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;

public class NotificationState implements AutoCloseable {
    private final NotificationDefinition notification;
    private final SubscriberSession client;

    public NotificationState(NotificationDefinition notification, String endpoint, NotificationMessageHandler handler,
            final TransportFactory transportFactory) throws URISyntaxException {
        this.notification = Preconditions.checkNotNull(notification);
        this.client = transportFactory.endpointBuilder().subscriber().useCache().create(endpoint, handler);
    }

    public NotificationDefinition notification() {
        return this.notification;
    }

    public SubscriberSession client() {
        return this.client;
    }

    @Override
    public void close() {
        client.close();
    }
}
