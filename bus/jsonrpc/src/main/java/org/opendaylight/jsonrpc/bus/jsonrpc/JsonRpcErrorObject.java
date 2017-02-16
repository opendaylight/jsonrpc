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
 * This class represents the internal error object that could be sent as part of
 * JSON RPC reply. This class should be wrapped inside a
 * {@link org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage JsonRpcMessageClass} class.
 * 
 * @author Shaleen Saxena
 */
public class JsonRpcErrorObject {
    private Integer code;
    private String message;
    private JsonElement data;

    public JsonRpcErrorObject() {
        message = null;
        code = null;
        data = null;
    }

    public JsonRpcErrorObject(Integer code, String message, JsonElement data) {
        this.message = message;
        this.code = code;
        this.data = data;
    }

    public JsonRpcErrorObject(JsonElement elem) {
        this();
        if (elem != null) {
            JsonObject obj = elem.getAsJsonObject();
            if (obj != null) {
                setCode(obj.get(JsonRpcConstants.CODE).getAsInt());
                setMessage(obj.get(JsonRpcConstants.MESSAGE).getAsString());
                setData(obj.get(JsonRpcConstants.DATA));
            }
        }
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code.intValue();
    }

    public void setCode(int code) {
        this.code = code;
    }

    public JsonElement getData() {
        return data;
    }

    public void setData(JsonElement data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "JsonRpcErrorObject [code=" + code + ", message=" + message
                + ", data=" + data + "]";
    }
}
