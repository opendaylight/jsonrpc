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

import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.api.Publisher;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcNotificationMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcSerializer;

/**
 * Implementation of {@link PublisherSession}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 24, 2018
 */
public class PublisherSessionImpl extends AbstractSession implements PublisherSession {
    private Publisher publisher;

    public PublisherSessionImpl(CloseCallback closeCallback, BusSessionFactory factory, String uri) {
        super(closeCallback);
        publisher = factory.publisher(uri);
        setAutocloseable(publisher);
    }

    @Override
    public void publish(String method, JsonElement params) {
        publish(method, params, null);
    }

    @Override
    public void publish(String method, JsonElement params, JsonObject metadata) {
        final JsonRpcNotificationMessage msg = JsonRpcNotificationMessage.builder()
                .params(params)
                .method(method)
                .metadata(metadata)
                .build();
        publisher.publish(JsonRpcSerializer.toJson(msg));
    }

    @Override
    public void publish(String method, Object params) {
        final JsonRpcNotificationMessage msg = JsonRpcNotificationMessage.builder()
                .paramsFromObject(params)
                .method(method)
                .build();
        publisher.publish(JsonRpcSerializer.toJson(msg));
    }
}
