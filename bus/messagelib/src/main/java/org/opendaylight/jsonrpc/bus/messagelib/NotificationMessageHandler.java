/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;

/**
 * This interface needs to be implemented by classes that wish to handle
 * notifications. There is no response needed after handling any notification.
 *
 * @author Shaleen Saxena
 */
public interface NotificationMessageHandler extends AutoCloseable {
    void handleNotification(JsonRpcRequestMessage notification);

    @Override
    default void close() throws Exception {
        // no-op
    }
}
