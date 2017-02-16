/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * This represents the JSON RPC Request Message. This can be a received message,
 * or used to construct a new Request message.
 * 
 * @author Shaleen Saxena
 */
public class JsonRpcRequestMessage extends JsonRpcBaseMessage {
    protected String method;
    protected JsonElement params;

    public JsonRpcRequestMessage() {
        // Create an empty message.
        // Fill fields later.
    }

    public JsonRpcRequestMessage(String jsonrpc, JsonElement id) {
        super(jsonrpc, id);
    }

    public JsonRpcRequestMessage(String jsonrpc, JsonElement id, String method, JsonElement params) {
        super(jsonrpc, id);
        this.method = method;
        this.params = params;
    }

    public JsonRpcRequestMessage(JsonElement id, String method, JsonElement params) {
        this(VERSION, id, method, params);
    }

    public boolean isNotification() {
        return id == null;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public JsonElement getParams() {
        return params;
    }

    public void setParams(JsonElement params) {
        this.params = params;
    }

    /**
     * This method can be used to convert the params to an object of the
     * specified class. This method works when the param is a single JSON
     * object.
     * 
     * @param cls The class to convert the params to.
     * @return The params as an object of the specified class
     * @throws JsonRpcException If the params do not match the specified class.
     */
    public <T> T getParamsAsObject(Class<T> cls) throws JsonRpcException {
        return convertJsonElementToClass(getParams(), cls);
    }

    /**
     * This method can be used to convert one element of the params to an object
     * of the specified class. This method works when the param is an array of
     * JSON objects.
     * 
     * @param index The index to get the object from.
     * @param cls The class to convert the params to.
     * @return The params as an object of the specified class
     * @throws JsonRpcException If the element in the params does not match the
     *             specified class.
     */
    public <T> T getParamsAtIndexAsObject(int index, Class<T> cls) throws JsonRpcException {
        if (params.isJsonArray()) {
            JsonArray paramArray = params.getAsJsonArray();
            if (index < paramArray.size()) {
                return convertJsonElementToClass(paramArray.get(index), cls);
            }
        } else if (index == 0) {
            return convertJsonElementToClass(params, cls);
        }
        return null;
    }

    /**
     * This method is used to set the parameters as an object. For an array
     * containing params of different types, use an Object[] to store them.
     * 
     * @param obj Object to store.
     */
    public void setParamsAsObject(Object obj) {
        setParams(convertClassToJsonElement(obj));
    }

    @Override
    public String toString() {
        return "JsonRpcRequest [jsonrpc=" + jsonrpc + ", id=" + id + ", method=" + method + ", params=" + params + "]";
    }

    @Override
    public JsonRpcMessageType getType() {
        if (isNotification()) {
            return JsonRpcMessageType.NOTIFICATION;
        }
        return JsonRpcMessageType.REQUEST;
    }
}
