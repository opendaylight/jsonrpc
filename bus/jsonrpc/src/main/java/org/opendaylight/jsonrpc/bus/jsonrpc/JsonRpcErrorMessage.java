/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import com.google.gson.JsonElement;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class is used when there are errors in parsing an incoming JSON RPC
 * message. This class contains sufficient information to build a
 * {@link org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage
 * JsonRpcReplyMessage} containing the error details.
 *
 * <p><a href="https://www.jsonrpc.org/specification#error_object">See message definition</a>
 *
 * @author Shaleen Saxena
 */
public final class JsonRpcErrorMessage extends JsonRpcBaseMessage {
    private final int code;
    private final String message;
    private final JsonElement data;

    private JsonRpcErrorMessage(Builder builder) {
        super(builder);
        this.code = builder.code;
        this.message = Objects.requireNonNull(builder.message);
        this.data = builder.data;
    }

    public @NonNull String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }

    public @Nullable JsonElement getData() {
        return data;
    }

    public <T> T getDataAsObject(Class<T> cls) throws JsonRpcException {
        return convertJsonElementToClass(getData(), cls);
    }

    @Override
    public JsonRpcMessageType getType() {
        return JsonRpcMessageType.PARSE_ERROR;
    }

    @Override
    public String toString() {
        return "JsonRpcMessageError [id=" + getId() + ", code=" + code + ", message=" + message
                + ", data=" + data + "]";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(JsonRpcErrorMessage copyFrom) {
        return new Builder(copyFrom);
    }

    public static class Builder extends AbstractBuilder<Builder, JsonRpcErrorMessage> {
        private int code;
        private String message;
        private JsonElement data;

        public Builder() {
            //default no-arg ctor
        }

        public Builder(JsonRpcErrorMessage copyFrom) {
            super(copyFrom);
            this.code = copyFrom.code;
            this.message = copyFrom.message;
            this.data = copyFrom.data;
        }

        public Builder code(int value) {
            this.code = value;
            return this;
        }

        public Builder message(String value) {
            this.message = value;
            return this;
        }

        public Builder data(JsonElement value) {
            this.data = value;
            return this;
        }

        public Builder dataFromObject(Object obj) {
            return data(convertClassToJsonElement(obj));
        }

        @Override
        protected JsonRpcErrorMessage newInstance() {
            return new JsonRpcErrorMessage(this);
        }
    }
}
