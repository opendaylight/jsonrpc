/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the top-level serializer and deserializer for JSON RPC messages
 * ({@link JsonRpcBaseMessage} and its derived classes). Ideally, there should
 * be one serializer for each derived class.
 *
 * @author Shaleen Saxena
 */
public final class JsonRpcSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcSerializer.class);

    private JsonRpcSerializer() {
        // empty constructor
    }

    private static JsonRpcBaseMessage processOneElement(JsonElement elem) {
        JsonObject obj = elem.getAsJsonObject();
        if (obj == null) {
            return JsonRpcMessageError.builder().code(-32700).message("Unable to parse object").build();
        }

        JsonElement id = obj.get(JsonRpcConstants.ID);

        JsonElement jsonrpcElem = obj.get(JsonRpcConstants.JSONRPC);
        if (jsonrpcElem == null || jsonrpcElem.isJsonNull()) {
            return JsonRpcMessageError.builder().id(id).code(-32700).message("JSON RPC version is not defined").build();
        }

        String jsonrpc = jsonrpcElem.getAsString();
        if (!JsonRpcBaseMessage.isSupportedVersion(jsonrpc)) {
            JsonObject data = new JsonObject();
            data.addProperty(JsonRpcConstants.JSONRPC, jsonrpc);
            return JsonRpcMessageError.builder().id(id).code(-32700).data(data)
                    .message("JSON RPC version is not supported").build();
        }

        if (obj.has(JsonRpcConstants.METHOD)) {
            // This is a Request or Notification - Verify it does not contain either result or error fields.
            if (obj.has(JsonRpcConstants.ERROR) || obj.has(JsonRpcConstants.RESULT)) {
                return JsonRpcMessageError.builder().id(id).code(-32700).message("Request message has error or result")
                        .build();
            }

            if (id != null) {
                return processRequestMessage(obj, id);
            } else {
                return processNotificationMessage(obj);
            }
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
                return JsonRpcMessageError.builder().id(id).code(-32700)
                        .message("Reply has both error and result").build();
            } else {
                // Valid result message
                if (obj.has(JsonRpcConstants.METADATA)) {
                    return JsonRpcReplyMessage.builder().id(id).result(obj.get(JsonRpcConstants.RESULT))
                            .metadata(obj.get(JsonRpcConstants.METADATA).getAsJsonObject()).build();
                } else {
                    return JsonRpcReplyMessage.builder().id(id).result(obj.get(JsonRpcConstants.RESULT)).build();
                }
            }
        } else if (obj.has(JsonRpcConstants.ERROR)) {
            // Valid error message
            return JsonRpcReplyMessage.builder().id(id).error(new JsonRpcErrorObject(obj.get(JsonRpcConstants.ERROR)))
                    .build();
        } else {
            // Unable to determine reply message type
            return JsonRpcMessageError.builder().id(id).code(-32700).message("Reply has neither error nor result")
                    .build();
        }
    }

    private static JsonRpcBaseMessage processRequestMessage(JsonObject obj, JsonElement id) {
        if (obj.has(JsonRpcConstants.METADATA)) {
            return JsonRpcRequestMessage.builder().id(id).method(obj.get(JsonRpcConstants.METHOD).getAsString())
                    .params(obj.get(JsonRpcConstants.PARAMS))
                    .metadata(obj.get(JsonRpcConstants.METADATA).getAsJsonObject()).build();
        } else {
            return JsonRpcRequestMessage.builder().id(id).method(obj.get(JsonRpcConstants.METHOD).getAsString())
                    .params(obj.get(JsonRpcConstants.PARAMS)).build();
        }
    }

    private static JsonRpcBaseMessage processNotificationMessage(JsonObject obj) {
        if (obj.has(JsonRpcConstants.METADATA)) {
            return JsonRpcNotificationMessage.builder().method(obj.get(JsonRpcConstants.METHOD).getAsString())
                    .params(obj.get(JsonRpcConstants.PARAMS))
                    .metadata(obj.get(JsonRpcConstants.METADATA).getAsJsonObject()).build();
        } else {
            return JsonRpcNotificationMessage.builder().method(obj.get(JsonRpcConstants.METHOD).getAsString())
                    .params(obj.get(JsonRpcConstants.PARAMS)).build();
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
        } catch (JsonSyntaxException e) {
            LOG.debug("Unable to parse JSON message", e);
            parsedJson = null;
        }

        if (parsedJson == null) {
            JsonRpcMessageError err = JsonRpcMessageError.builder().code(-32700)
                    .message("Unable to parse incoming message").build();
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
            JsonRpcMessageError err = JsonRpcMessageError.builder().code(-32700)
                    .message("Unable to determine incoming message").build();
            list.add(err);
        }

        return list;
    }

    private static String toJson(Object obj) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(JsonRpcMessageError.class, new JsonRpcMessageErrorSerializer())
                .registerTypeAdapter(JsonRpcReplyMessage.class, new JsonRpcReplyMessageSerializer())
                .registerTypeAdapter(JsonRpcRequestMessage.class, new JsonRpcRequestMessageSerializer())
                .registerTypeAdapter(JsonRpcNotificationMessage.class, new JsonRpcNotificationMessageSerializer())
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
