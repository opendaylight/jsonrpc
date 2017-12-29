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

/**
 * This represents the JSON RPC Reply message. This can be the received message,
 * or used to construct a new Reply message. Both Result and Error component of
 * the Reply message may be set in this class, which is an invalid message. This
 * could be useful in negative testing, or to receive an incorrect message.
 *
 * @author Shaleen Saxena
 */
public class JsonRpcReplyMessage extends JsonRpcBaseMessage {
    private JsonElement result;
    private JsonRpcErrorObject error;

    public JsonRpcReplyMessage() {
        // Create an empty message.
        // Fill fields later.
    }

    public JsonRpcReplyMessage(String jsonrpc, JsonElement id, JsonElement result, JsonRpcErrorObject error,
            JsonObject metadata) {
        super(jsonrpc, id, metadata);
        this.result = result;
        this.error = error;

        if (result != null && error != null) {
            throw new IllegalArgumentException("Both result and error defined");
        }
    }

    public JsonRpcReplyMessage(JsonElement id, JsonElement result, JsonObject metadata) {
        this(VERSION, id, result, null, metadata);
    }

    public JsonRpcReplyMessage(JsonElement id, JsonElement result) {
        this(VERSION, id, result, null, null);
    }

    public JsonRpcReplyMessage(JsonElement id, JsonRpcErrorObject error) {
        this(VERSION, id, null, error, null);
    }

    public boolean isResult() {
        return result != null;
    }

    public boolean isError() {
        return error != null;
    }

    public JsonElement getResult() {
        return result;
    }

    public void setResult(JsonElement result) {
        this.result = result;
    }

    /**
     * This returns the result part of the Reply message as the user supplied
     * object. Might throw an exception if the values in the Reply do not match.
     *
     * @param cls The class of expected result object.
     * @return The result as an object of the provided class.
     * @throws JsonRpcException If the result part of message does not match the
     *             class.
     */
    public <T> T getResultAsObject(Class<T> cls) throws JsonRpcException {
        return convertJsonElementToClass(getResult(), cls);
    }

    /**
     * This sets the result part of the Reply message as the user supplied
     * object.
     *
     * @param obj The user supplied result object.
     */
    public void setResultAsObject(Object obj) {
        setResult(convertClassToJsonElement(obj));
    }

    public JsonRpcErrorObject getError() {
        return error;
    }

    public void setError(JsonRpcErrorObject error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "JsonRpcReplyMessage [jsonrpc=" + jsonrpc + ", id=" + id + ",result=" + result + ", error=" + error
                + "]";
    }

    @Override
    public JsonRpcMessageType getType() {
        return JsonRpcMessageType.REPLY;
    }
}
