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
import org.opendaylight.jsonrpc.bus.messagelib.EndpointRole;
import org.opendaylight.jsonrpc.bus.messagelib.MessageLibraryException;
import org.opendaylight.jsonrpc.bus.messagelib.Session;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.impl.JsonRPCHandler;
import org.opendaylight.jsonrpc.impl.Util;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

public class RpcState {
    private final String name;
    private final RpcDefinition rpc;
    private final Session client;
    private final JsonRPCHandler handler;

    public RpcState(String qname, RpcDefinition rpc, String endpoint, TransportFactory transportFactory)
            throws URISyntaxException {
        this.name = Preconditions.checkNotNull(qname);
        Preconditions.checkNotNull(endpoint);
        this.rpc = Preconditions.checkNotNull(rpc);
        this.client = transportFactory.createSession(Util.ensureRole(endpoint, EndpointRole.REQ));
        this.handler = new JsonRPCHandler();
        this.client.setReplyMessageHandler(handler);
    }

    public RpcDefinition rpc() {
        return this.rpc;
    }

    public JsonElement lastMessage() {
        return this.handler.getResult();
    }

    public JsonObject lastMetadata() {
        if (this.handler.getMetadata() != null) {
            return this.handler.getMetadata().getAsJsonObject();
        } else {
            return null;
        }
    }

    public JsonRpcErrorObject lastError() {
        return this.handler.getError();
    }

    public JsonElement sendRequest(JsonElement argument, JsonObject metadata) {
        /* we will refine the handling here later */
        try {
            if (metadata == null) {
                this.client.sendRequest(this.name, argument);
            } else {
                this.client.sendRequest(this.name, argument, metadata);
            }
            int replyCount = this.client.handleIncomingMessage();
            if (replyCount > 0) {
                return this.lastMessage();
            } else {
                return null;
            }
        } catch (MessageLibraryException e) {
            return null;
        }
    }
}
