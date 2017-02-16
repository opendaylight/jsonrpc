/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Abstract base class for JSON RPC messages. The derived classes are used for
 * the actual JSON RPC messages. These classes can be used for both sending and
 * receiving messages. When used for sending messages, a custom serializer
 * should be written for Object to JSON conversion.
 * 
 * @author Shaleen Saxena
 * 
 */
public abstract class JsonRpcBaseMessage {
    public static final Logger logger = LoggerFactory.getLogger(JsonRpcBaseMessage.class);
    private static final Gson gson = new Gson();
    protected static final String VERSION = "2.0";
    protected static final String VERSION_SHORT = "2";
    protected String jsonrpc;
    protected JsonElement id;

    public enum JsonRpcMessageType {
        REQUEST,      // A request which expects a reply
        NOTIFICATION, // A notification which has no reply
        REPLY,        // Reply with either a result or error in it
        PARSE_ERROR   // Incoming invalid message is marshaled into this pseudo-message
    }

    public JsonRpcBaseMessage() {
        // Create an empty message.
        // Fill fields later.
    }

    public JsonRpcBaseMessage(String jsonrpc, JsonElement id) {
        this.jsonrpc = jsonrpc;
        this.id = id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public void setDefaultJsonrpc() {
        this.jsonrpc = VERSION;
    }

    public JsonElement getId() {
        return id;
    }

    public void setId(JsonElement id) {
        this.id = id;
    }

    public void setIdAsIntValue(int id) {
        this.id = new JsonPrimitive(id);
    }

    public int getIdAsIntValue() {
        try {
            return getId().getAsJsonPrimitive().getAsNumber().intValue();
        } catch (Exception e) {
            logger.debug("Unable to parse ID as int", e);
        }
        return 0;
    }

    /*
     * Convenience function for converting Gson's JsonElement to an object. The
     * conversion is achieved by converting JsonElement to JSON and then parsed
     * as the requested object. Perhaps not very efficient, but very convenient
     * though.
     */
    @SuppressWarnings("unchecked")
    protected static <T> T convertJsonElementToClass(JsonElement elem,
            Class<T> cls) throws JsonRpcException {
        if (cls.isInstance(elem)) {
            return (T) elem; 
        } 
        try {
            return gson.fromJson(gson.toJson(elem), cls);
        } catch (Exception e) {
            throw new JsonRpcException(e);
        }
    }

    /*
     * Convenience function for converting an Object to Gson's JsonElement. The
     * conversion is achieved by converting Object to JSON and then parsed back
     * as JsonElement. Perhaps not very efficient, but very convenient though.
     */
    protected static JsonElement convertClassToJsonElement(Object obj) {
        if (JsonElement.class.isInstance(obj)) {
            return (JsonElement) obj;
        } else {
            return gson.fromJson(gson.toJson(obj), JsonElement.class);
        }
    }

    public static String getSupportedVersion() {
        return VERSION;
    }

    public static boolean isSupportedVersion(String version) {
        return VERSION.equals(version) || (VERSION_SHORT.equals(version));
    }

    public abstract JsonRpcMessageType getType();
}
