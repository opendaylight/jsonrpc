/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import java.util.List;
import java.util.Objects;

import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.PeerContext;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcBaseMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcBaseMessage.JsonRpcMessageType;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcNotificationMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for {@link NotificationMessageHandler}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 27, 2018
 */
public class NotificationHandlerAdapter implements MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationHandlerAdapter.class);
    private final NotificationMessageHandler handler;

    public NotificationHandlerAdapter(final NotificationMessageHandler handler) {
        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public void onMessage(final PeerContext peerContext, final String message) {
        LOG.debug("Notification : {}", message);
        final List<JsonRpcBaseMessage> incoming = JsonRpcSerializer.fromJson(message);
        for (final JsonRpcBaseMessage notification : incoming) {
            if (notification.getType() != JsonRpcMessageType.NOTIFICATION) {
                throw new MessageLibraryMismatchException(
                        String.format("Expected NOTIFICATION, but got %s message", notification.getType().name()));
            } else {
                handler.handleNotification((JsonRpcNotificationMessage) notification);
            }
        }
    }
}
