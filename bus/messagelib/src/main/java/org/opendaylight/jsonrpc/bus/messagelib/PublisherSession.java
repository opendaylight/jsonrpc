/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public interface PublisherSession extends BaseSession {
    /**
     * Publish notification.
     *
     * @param method notification method
     * @param params notification parameters
     */
    void publish(String method, Object params);

    /**
     * Publish notification.
     *
     * @param method notification method
     * @param params notification parameters
     */
    void publish(String method, JsonElement params);

    /**
     * Publish notification with metadata.
     *
     * @param method notification method
     * @param params notification parameters
     * @param metadata additional metadata
     */
    void publish(String method, JsonElement params, JsonObject metadata);
}