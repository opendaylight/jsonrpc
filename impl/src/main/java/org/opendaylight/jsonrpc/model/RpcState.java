/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URISyntaxException;

import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcException;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.messagelib.NoopReplyMessageHandler;
import org.opendaylight.jsonrpc.bus.messagelib.RequesterSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

public class RpcState implements AutoCloseable {
    private final String name;
    private final RpcDefinition rpc;
    private final RequesterSession client;
    private JsonElement result;
    private JsonObject metadata;
    private JsonRpcErrorObject error;

    public RpcState(String qname, RpcDefinition rpc, String endpoint, TransportFactory transportFactory)
            throws URISyntaxException {
        this.name = Preconditions.checkNotNull(qname);
        Preconditions.checkNotNull(endpoint);
        this.rpc = Preconditions.checkNotNull(rpc);
        this.client = transportFactory.createRequester(endpoint, NoopReplyMessageHandler.INSTANCE, false);
    }

    public RpcDefinition rpc() {
        return this.rpc;
    }

    public JsonElement lastMessage() {
        return result;
    }

    public JsonObject lastMetadata() {
        if (this.metadata != null) {
            return this.metadata.getAsJsonObject();
        } else {
            return null;
        }
    }

    public JsonRpcErrorObject lastError() {
        return this.error;
    }

    public JsonElement sendRequest(JsonElement argument, JsonObject metadataParam) {
        final JsonRpcReplyMessage reply = client.sendRequestAndReadReply(name, argument, metadataParam);
        if (!reply.isError()) {
            try {
                result = reply.getResultAsObject(JsonElement.class);
                metadata = reply.getMetadata();
                error = null;
            } catch (JsonRpcException e) {
                result = null;
                error = reply.getError();
            }
        } else {
            result = null;
            error = reply.getError();
        }
        return result;
    }

    @Override
    public void close() {
        client.close();
    }
}
