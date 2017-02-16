/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcException;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.messagelib.*;

import com.google.gson.JsonElement;

public class JsonRPCHandler implements ReplyMessageHandler { 
    private JsonElement result;
    private JsonRpcErrorObject error;

    public JsonElement getResult() {
        return this.result;
    }
    public JsonElement setResult(JsonElement result) {
        this.result = result;
        return result;
    }

    public JsonRpcErrorObject getError() {
        return this.error;
    }
    public JsonRpcErrorObject setError(JsonRpcErrorObject error) {
        this.error = error;
        return error;
    }

    @Override
    public void handleReply (JsonRpcReplyMessage reply) {
        try {
            if (! reply.isError()) {
                this.setResult(reply.getResultAsObject(JsonElement.class));
                this.setError(null);
            } else {
                throw new JsonRpcException("AAAAArghhh...");
            }
        } catch (JsonRpcException e) {
                this.setResult(null);
                this.setError(reply.getError()); 
        }
    }
}

