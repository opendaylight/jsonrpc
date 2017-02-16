/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

public class RpcMessageMapResultTest {
    private static final Logger logger = LoggerFactory.getLogger(RpcMessageMapResultTest.class);

    @Test
    public void testResultAsMap() {
        JsonRpcReplyMessage reply1 = new JsonRpcReplyMessage();
        String[] params = { "first", "second" };
        Map<String, Object> map = new HashMap<String, Object>();

        for (String param : params) {
            map.put(param, "Hello " + param);
        }

        logger.info("Class: ", map.getClass().toString());

        reply1.setDefaultJsonrpc();
        reply1.setIdAsIntValue(1);
        reply1.setResultAsObject(map);

        String replyStr = JsonRpcSerializer.toJson(reply1);
        logger.info(replyStr);

        // Map<String,Object> result;
        List<JsonRpcBaseMessage> msgs = JsonRpcSerializer.fromJson(replyStr);
        for (JsonRpcBaseMessage msg : msgs) {
            JsonRpcReplyMessage reply2 = (JsonRpcReplyMessage) msg;
            logger.info(reply2.toString());
            JsonElement result = reply2.getResult();
            logger.info(result.toString());
        }
    }
}
