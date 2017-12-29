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
 * This is the serializer for the JSON RPC Reply Message. This does not verify
 * that only one of Result or Error parameter is set. Hence an invalid JSON RPC
 * Reply can be created. This behavior is useful for negative tests.
 *
 * @author Shaleen Saxena
 */
public class JsonRpcReplyMessageSerializer extends Object implements JsonSerializer<JsonRpcReplyMessage> {

    @Override
    public JsonElement serialize(JsonRpcReplyMessage src, Type typeOfSrc, JsonSerializationContext context) {
        final JsonObject obj = new JsonObject();
        obj.addProperty(JsonRpcConstants.JSONRPC, src.getJsonrpc());
        obj.add(JsonRpcConstants.ID, src.getId());
        if (src.getError() != null) {
            JsonObject err = new JsonObject();
            err.addProperty(JsonRpcConstants.CODE, src.getError().getCode());
            err.addProperty(JsonRpcConstants.MESSAGE, src.getError().getMessage());
            if (src.getError().getData() != null) {
                err.add(JsonRpcConstants.DATA, src.getError().getData());
            }
            obj.add(JsonRpcConstants.ERROR, err);
        }
        if (src.getResult() != null) {
            obj.add(JsonRpcConstants.RESULT, src.getResult());
        }
        if (src.getMetadata() != null) {
            obj.add(JsonRpcConstants.METADATA, src.getMetadata());
        }
        return obj;
    }
}
