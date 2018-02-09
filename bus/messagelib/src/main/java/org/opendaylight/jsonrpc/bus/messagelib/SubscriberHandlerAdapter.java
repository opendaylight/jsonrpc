/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcNotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for {@link NotificationMessageHandler} used in subscriber proxy.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Apr 3, 2018
 */
public class SubscriberHandlerAdapter extends AbstractProxyHandlerAdapter implements NotificationMessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SubscriberHandlerAdapter.class);

    public SubscriberHandlerAdapter(Object handler) {
        super(true, handler);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public void handleNotification(JsonRpcNotificationMessage notification) {
        if (handler instanceof NotificationMessageHandler) {
            ((NotificationMessageHandler) handler).handleNotification(notification);
        } else {
            try {
                invokeHandler(notification);
            } catch (Exception e) {
                // no way to report problem as notification can't send reply
                LOG.debug("Error while invoking notification method", e);
            }
        }
    }
}
