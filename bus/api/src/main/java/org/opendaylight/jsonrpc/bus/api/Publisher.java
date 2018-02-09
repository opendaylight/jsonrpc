/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.api;

/**
 * Publisher session type.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 4, 2018
 */
public interface Publisher extends ServerSession {
    /**
     * Publish message to any topic.
     *
     * @param message message to publish.
     */
    default void publish(String message) {
        publish(message, "");
    }

    /**
     * Publish message to specific topic.
     *
     * @param message message to publish.
     * @param topic topic of message.
     */
    void publish(String message, String topic);
}
