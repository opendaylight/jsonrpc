/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import java.util.Objects;

/**
 * This represents the JSON RPC Request Message. This can be a received message,
 * or used to construct a new Request message.
 *
 * @author Shaleen Saxena
 */
public final class JsonRpcRequestMessage extends JsonRpcBaseRequestMessage {

    private JsonRpcRequestMessage(Builder builder) {
        super(builder);
        Objects.requireNonNull(getId());
    }

    @Override
    public JsonRpcMessageType getType() {
        return JsonRpcMessageType.REQUEST;
    }

    @Override
    public String toString() {
        return "JsonRpcRequestMessage [jsonrpc=" + getJsonrpc() + ", id=" + getId() + ", method=" + getMethod()
                + ", params=" + getParams() + "]";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractRequestBuilder<Builder, JsonRpcRequestMessage> {
        @Override
        protected JsonRpcRequestMessage newInstance() {
            return new JsonRpcRequestMessage(this);
        }
    }
}
