/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import com.google.gson.JsonElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcMessageMapResultTest {
    private static final Logger LOG = LoggerFactory.getLogger(RpcMessageMapResultTest.class);

    @Test
    public void testResultAsMap() {
        JsonRpcReplyMessage reply1 = new JsonRpcReplyMessage();
        String[] params = { "first", "second" };
        Map<String, Object> map = new HashMap<>();

        for (String param : params) {
            map.put(param, "Hello " + param);
        }

        LOG.info("Class: ", map.getClass().toString());

        reply1.setDefaultJsonrpc();
        reply1.setIdAsIntValue(1);
        reply1.setResultAsObject(map);

        String replyStr = JsonRpcSerializer.toJson(reply1);
        LOG.info(replyStr);

        // Map<String,Object> result;
        List<JsonRpcBaseMessage> msgs = JsonRpcSerializer.fromJson(replyStr);
        for (JsonRpcBaseMessage msg : msgs) {
            JsonRpcReplyMessage reply2 = (JsonRpcReplyMessage) msg;
            LOG.info(reply2.toString());
            JsonElement result = reply2.getResult();
            LOG.info(result.toString());
        }
    }
}
