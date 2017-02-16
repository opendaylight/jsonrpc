/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * This is the serializer for the JSON RPC Request Message. This does not verify
 * that method or parameters are set. Hence an invalid JSON RPC Request can be
 * created. This behavior is useful for negative tests.
 * 
 * @author Shaleen Saxena
 */
public class JsonRpcRequestMessageSerializer extends Object implements JsonSerializer<JsonRpcRequestMessage> {

    @Override
    public JsonElement serialize(JsonRpcRequestMessage src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty(JsonRpcConstants.JSONRPC, src.getJsonrpc());

        if (src.getId() != null) {
            obj.add(JsonRpcConstants.ID, src.getId());
        }

        if (src.getMethod() != null) {
            obj.addProperty(JsonRpcConstants.METHOD, src.getMethod());
        }

        if (src.getParams() != null) {
            obj.add(JsonRpcConstants.PARAMS, src.getParams());
        }
        return obj;
    }
}
