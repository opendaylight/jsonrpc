/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
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
 * This serializer extracts the error data to form a JSON RPC with error.
 *
 * @author Shaleen Saxena
 */
public class JsonRpcMessageErrorSerializer implements JsonSerializer<JsonRpcMessageError> {
    @Override
    public JsonElement serialize(JsonRpcMessageError src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject err = new JsonObject();
        err.addProperty(JsonRpcConstants.CODE, Integer.valueOf(src.getCode()));
        err.addProperty(JsonRpcConstants.MESSAGE, src.getMessage());
        err.add(JsonRpcConstants.DATA, src.getData());

        JsonObject obj = new JsonObject();
        obj.addProperty(JsonRpcConstants.JSONRPC, JsonRpcBaseMessage.getSupportedVersion());
        obj.add(JsonRpcConstants.ID, src.getId());
        obj.add(JsonRpcConstants.ERROR, err);
        return obj;
    }
}
