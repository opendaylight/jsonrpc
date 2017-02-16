/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import com.google.gson.JsonElement;

/**
 * This class is used when there are errors in parsing an incoming JSON RPC
 * message. This class contains sufficient information to build a
 * {@link org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage
 * JsonRpcReplyMessage} containing the error details.
 * 
 * @author Shaleen Saxena
 */
public class JsonRpcMessageError extends JsonRpcBaseMessage {
    private Integer code;
    private String message;
    private JsonElement data;

    public JsonRpcMessageError() {
        id = null;
        message = null;
        code = null;
        data = null;
    }

    public JsonRpcMessageError(JsonElement id, Integer code, String message, JsonElement data) {
        setDefaultJsonrpc();
        this.id = id;
        this.message = message;
        this.code = code;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public JsonElement getData() {
        return data;
    }

    public void setData(JsonElement data) {
        this.data = data;
    }

    public <T> T getDataAsObject(Class<T> cls) throws JsonRpcException {
        return convertJsonElementToClass(getData(), cls);
    }

    public void setDataAsObject(Object obj) throws JsonRpcException {
        setData(convertClassToJsonElement(obj));
    }

    @Override
    public JsonElement getId() {
        return id;
    }

    @Override
    public void setId(JsonElement id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "JsonRpcMessageError [id=" + id + ", code=" + code + ", message=" + message + ", data=" + data + "]";
    }

    @Override
    public JsonRpcMessageType getType() {
        return JsonRpcMessageType.PARSE_ERROR;
    }
}
