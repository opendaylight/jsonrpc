/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * This is the top-level serializer and deserializer for JSON RPC messages
 * ({@link JsonRpcBaseMessage} and its derived classes). Ideally, there should
 * be one serializer for each derived class.
 * 
 * @author Shaleen Saxena
 */
public class JsonRpcSerializer {
    private static final Logger logger = LoggerFactory.getLogger(JsonRpcSerializer.class);

    private JsonRpcSerializer() {
        // empty constructor
    }

    private static JsonRpcBaseMessage processOneElement(JsonElement elem) {
        JsonObject obj = elem.getAsJsonObject();
        if (obj == null) {
            return new JsonRpcMessageError(null, -32700, "Unable to parse object", null);
        }

        JsonElement id = obj.get(JsonRpcConstants.ID);

        JsonElement jsonrpcElem = obj.get(JsonRpcConstants.JSONRPC);
        if ((jsonrpcElem == null) || (jsonrpcElem.isJsonNull())) {
            return new JsonRpcMessageError(id, -32700, "JSON RPC version is not defined", null);
        }

        String jsonrpc = jsonrpcElem.getAsString();
        if (!JsonRpcBaseMessage.isSupportedVersion(jsonrpc)) {
            JsonObject data = new JsonObject();
            data.addProperty(JsonRpcConstants.JSONRPC, jsonrpc);
            return new JsonRpcMessageError(id, -32700, "JSON RPC version is not supported", data);
        }

        if (obj.has(JsonRpcConstants.METHOD)) {
            // This is a request message
            return processRequestMessage(obj, id);
        } else {
            // This is a reply message
            return processReplyMessage(obj, id);
        }
    }

    private static JsonRpcBaseMessage processReplyMessage(JsonObject obj, JsonElement id) {
        // Verify that Reply does not contain both result and error fields
        if (obj.has(JsonRpcConstants.RESULT)) {
            if (obj.has(JsonRpcConstants.ERROR)) {
                // Invalid message with result and error fields
                return new JsonRpcMessageError(id, -32700, "Reply has both error and result", null);
            } else {
                // Valid result message
                if (obj.has(JsonRpcConstants.METADATA)) {
                    return new JsonRpcReplyMessage(id, obj.get(JsonRpcConstants.RESULT), obj.get(JsonRpcConstants.METADATA).getAsJsonObject());
                } else {
                    return new JsonRpcReplyMessage(id, obj.get(JsonRpcConstants.RESULT));
                }
            }
        } else if (obj.has(JsonRpcConstants.ERROR)) {
            // Valid error message
            return new JsonRpcReplyMessage(id, new JsonRpcErrorObject(obj.get(JsonRpcConstants.ERROR)));
        } else {
            // Unable to determine reply message type
            return new JsonRpcMessageError(id, -32700, "Reply has neither error nor result", null);
        }
    }

    private static JsonRpcBaseMessage processRequestMessage(JsonObject obj, JsonElement id) {
        // Verify that Request does not contain either result or error fields.
        if (obj.has(JsonRpcConstants.ERROR) || obj.has(JsonRpcConstants.RESULT)) {
            return new JsonRpcMessageError(id, -32700, "Request message has error or result", null);
        } else {
            if (obj.has(JsonRpcConstants.METADATA)) {
                return new JsonRpcRequestMessage(id, obj.get(JsonRpcConstants.METHOD).getAsString(),
                        obj.get(JsonRpcConstants.PARAMS),  obj.get(JsonRpcConstants.METADATA).getAsJsonObject());
            } else {
                return new JsonRpcRequestMessage(id, obj.get(JsonRpcConstants.METHOD).getAsString(),
                        obj.get(JsonRpcConstants.PARAMS));
            }
        }
    }

    /**
     * Parses an incoming JSON RPC message. This can handle either a single
     * message or an array of messages. Hence, the return value is a list of
     * {@link JsonRpcBaseMessage}, even for single messages. If a message cannot
     * be parsed, then a {@link JsonRpcMessageError} object is returned.
     * 
     * @param strJson Incoming String containing JSON RPC message.
     * @return Returns a list of messages.
     */
    public static List<JsonRpcBaseMessage> fromJson(String strJson) {
        JsonElement parsedJson;
        Gson gson = new Gson();
        List<JsonRpcBaseMessage> list = new ArrayList<>();

        try {
            parsedJson = gson.fromJson(strJson, JsonElement.class);
        } catch (Exception e) {
            logger.debug("Unable to parse JSON message", e);
            JsonRpcMessageError err = new JsonRpcMessageError(null, -32700, "Unable to parse incoming message", null);
            list.add(err);
            return list;
        }

        if (parsedJson.isJsonArray()) {
            for (JsonElement e : parsedJson.getAsJsonArray()) {
                list.add(processOneElement(e));
            }
        } else if (parsedJson.isJsonObject()) {
            list.add(processOneElement(parsedJson));
        } else {
            JsonRpcMessageError err = new JsonRpcMessageError(null, -32700, "Unable to determine incoming message",
                    null);
            list.add(err);
        }

        return list;
    }

    private static String toJson(Object obj) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(JsonRpcMessageError.class, new JsonRpcMessageErrorSerializer())
                .registerTypeAdapter(JsonRpcReplyMessage.class, new JsonRpcReplyMessageSerializer())
                .registerTypeAdapter(JsonRpcRequestMessage.class, new JsonRpcRequestMessageSerializer())
                .serializeNulls().create();
        return gson.toJson(obj);
    }

    public static String toJson(JsonRpcBaseMessage msg) {
        return toJson((Object) msg);
    }

    public static String toJson(List<JsonRpcBaseMessage> msg) {
        return toJson(msg.toArray());
    }
}
