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
import java.lang.reflect.Type;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This represents the JSON RPC Reply message. This can be the received message,
 * or used to construct a new Reply message. Both Result and Error component of
 * the Reply message may be set in this class, which is an invalid message. This
 * could be useful in negative testing, or to receive an incorrect message.
 *
 * @author Shaleen Saxena
 */
public final class JsonRpcReplyMessage extends JsonRpcBaseMessage {
    private final JsonElement result;
    private final JsonRpcErrorObject error;

    private JsonRpcReplyMessage(Builder builder) {
        super(builder);
        this.result = builder.result;
        this.error = builder.error;
    }

    public boolean isResult() {
        return result != null;
    }

    public boolean isError() {
        return error != null;
    }

    public @Nullable JsonElement getResult() {
        return result;
    }

    /**
     * This returns the result part of the Reply message as the user supplied
     * object. Might throw an exception if the values in the Reply do not match.
     *
     * @param type The {@link Type} of expected result object.
     * @return The result as an object of the provided class.
     * @throws JsonRpcException If the result part of message does not match the
     *             class.
     */
    public <T> T getResultAsObject(Type type) throws JsonRpcException {
        return convertJsonElementToClass(getResult(), type);
    }

    public @Nullable JsonRpcErrorObject getError() {
        return error;
    }

    @Override
    public JsonRpcMessageType getType() {
        return JsonRpcMessageType.REPLY;
    }

    @Override
    public String toString() {
        return "JsonRpcReplyMessage [jsonrpc=" + getJsonrpc() + ", id=" + getId() + ",result=" + result
                + ", error=" + error + "]";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(JsonRpcReplyMessage copyFrom) {
        return new Builder(copyFrom);
    }

    public static class Builder extends AbstractBuilder<Builder, JsonRpcReplyMessage> {
        private JsonElement result;
        private JsonRpcErrorObject error;

        public Builder() {
            //default no-args ctor
        }

        public Builder(JsonRpcReplyMessage copyFrom) {
            super(copyFrom);
            this.result = copyFrom.result;
            this.error = copyFrom.error;
        }

        public Builder result(JsonElement value) {
            this.result = value;
            return this;
        }

        /**
         * This sets the result part of the Reply message as the user supplied
         * object.
         *
         * @param obj The user supplied result object.
         */
        public Builder resultFromObject(Object obj) {
            return result(convertClassToJsonElement(obj));
        }

        public Builder error(JsonRpcErrorObject value) {
            this.error = value;
            return this;
        }

        @Override
        protected JsonRpcReplyMessage newInstance() {
            if (result != null && error != null) {
                throw new IllegalArgumentException("Both result and error defined");
            }

            if (result == null && error == null) {
                // no result or error was set so set a dummy value.
                result(new JsonObject());
            }

            return new JsonRpcReplyMessage(this);
        }
    }
}
