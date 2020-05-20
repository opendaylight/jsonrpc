/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import com.google.gson.JsonObject;

/**
 * DOM Notification publisher.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Apr 27, 2019
 */
public interface RemoteNotificationPublisher {
    /**
     * Publish notification to all registered subscribers.
     *
     * @param name name of notification (can be prefixed by module name)
     * @param data notification content
     */
    void publishNotification(String name, JsonObject data);
}
