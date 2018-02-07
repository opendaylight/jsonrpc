/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

/**
 * This is the serializer for the JSON RPC Notification Message.
 *
 * @author Thomas Pantelis
 */
public class JsonRpcNotificationMessageSerializer implements JsonSerializer<JsonRpcNotificationMessage> {
    @Override
    public JsonElement serialize(JsonRpcNotificationMessage src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty(JsonRpcConstants.JSONRPC, src.getJsonrpc());

        obj.addProperty(JsonRpcConstants.METHOD, src.getMethod());

        if (src.getParams() != null) {
            obj.add(JsonRpcConstants.PARAMS, src.getParams());
        }

        if (src.getMetadata() != null) {
            obj.add(JsonRpcConstants.METADATA, src.getMetadata());
        }
        return obj;
    }
}
