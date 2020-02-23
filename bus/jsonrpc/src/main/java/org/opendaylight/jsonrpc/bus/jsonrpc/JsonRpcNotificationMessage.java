/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

/**
 * Represents the JSON RPC notification message.
 *
 * @author Thomas Pantelis
 */
public final class JsonRpcNotificationMessage extends JsonRpcBaseRequestMessage {

    private JsonRpcNotificationMessage(Builder builder) {
        super(builder);
        if (getId() != null) {
            throw new IllegalArgumentException("Notification message must not have an id");
        }
    }

    @Override
    public JsonRpcMessageType getType() {
        return JsonRpcMessageType.NOTIFICATION;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(JsonRpcNotificationMessage copyFrom) {
        return new Builder(copyFrom);
    }

    public static class Builder extends AbstractRequestBuilder<Builder, JsonRpcNotificationMessage> {
        public Builder() {
            //default no-args ctor
        }

        public Builder(JsonRpcNotificationMessage copyFrom) {
            super(copyFrom);
        }

        @Override
        protected JsonRpcNotificationMessage newInstance() {
            return new JsonRpcNotificationMessage(this);
        }
    }
}
