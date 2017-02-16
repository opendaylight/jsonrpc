/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import java.net.URISyntaxException;

import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject;
import org.opendaylight.jsonrpc.bus.messagelib.EndpointRole;
import org.opendaylight.jsonrpc.bus.messagelib.Session;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.impl.JsonRPCHandler;
import org.opendaylight.jsonrpc.impl.Util;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;

public class RpcState {
    private String name;
    private RpcDefinition rpc;
    private Session client;
    private JsonRPCHandler handler;

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

    public JsonRpcErrorObject lastError() {
        return this.handler.getError();
    }

    public JsonElement sendRequest(JsonElement argument) {
        /* we will refine the handling here later */
        try {
            this.client.sendRequest(this.name, argument);
            int replyCount = this.client.handleIncomingMessage();
            if (replyCount > 0) {
                return this.lastMessage();
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
