/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage.Builder;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.opendaylight.jsonrpc.bus.messagelib.RequestMessageHandler;

/**
 * Mock RPC implementation with asynchronous extension support.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since May 6, 2018
 */
public class AsyncMockRpcHandler implements RequestMessageHandler, AutoCloseable {
    private static final int TOTAL_POLLS = 5;
    private volatile int counter = TOTAL_POLLS;
    private int in;

    @Override
    public void close() throws Exception {
    }

    @Override
    public void handleRequest(JsonRpcRequestMessage request, Builder replyBuilder) {
        replyBuilder.metadata(request.getMetadata());
        replyBuilder.result(JsonNull.INSTANCE);
        // first poll, grab input parameter
        if (counter == TOTAL_POLLS) {
            in = request.getParams().getAsJsonObject().get("in-number").getAsInt();
        }
        counter--;
        // last poll, send real result
        if (counter == 0) {
            int out = 1;
            for (int i = 2; i <= in; i++) {
                out *= i;
            }
            JsonObject obj = new JsonObject();
            obj.addProperty("out-number", out);
            replyBuilder.result(obj);
        }
    }
}
