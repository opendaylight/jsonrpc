/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRpcSerializerTest {
    private static Logger logger;

    @BeforeClass
    public static void setup() {
        logger = LoggerFactory.getLogger(JsonRpcSerializerTest.class);
    }

    @Test
    public void testIntParamArray() {
        String msg = "{\"jsonrpc\": \"2.0\", \"method\": \"subtract\", \"params\": [42, 23], \"id\": 1}";

        logger.info(msg);

        List<JsonRpcBaseMessage> list = JsonRpcSerializer.fromJson(msg);
        for (JsonRpcBaseMessage elem : list) {
            RpcMessageIntArrayParams intArrMsg = new RpcMessageIntArrayParams(
                    (JsonRpcRequestMessage) elem);
            logger.info(intArrMsg.toString());
            assertEquals(intArrMsg.getParams()[0], 42);
            assertEquals(intArrMsg.getParams()[1], 23);
        }
    }

    @Test
    public void testParamAsStringArray() {
        String[] param1 = {"first", "second"};
        JsonRpcRequestMessage request1 = JsonRpcRequestMessage.builder().idFromIntValue(1).method("echo")
                .paramsFromObject(param1).build();

        String requestStr = JsonRpcSerializer.toJson(request1);
        logger.info(requestStr);

        List<JsonRpcBaseMessage> list = JsonRpcSerializer.fromJson(requestStr);
        assertEquals(1, list.size());
        JsonRpcBaseMessage elem = list.get(0);
        assertTrue(elem instanceof JsonRpcRequestMessage);

        JsonRpcRequestMessage request2 = (JsonRpcRequestMessage) elem;
        try {
            assertEquals("echo", request2.getMethod());
            String[] param2 = request2.getParamsAsObject(String[].class);
            assertEquals("first", param2[0]);
            assertEquals("second", param2[1]);
        } catch (JsonRpcException e) {
            logger.error("Test error", e);
            fail("Unexpected exception");
        }
    }

    public <T> void testMessagesHelper(String[] msgs, int expectedCount,
            Class<T> expectedClass) {
        int count = 0;

        for (String msg : msgs) {
            logger.info(msg);

            List<JsonRpcBaseMessage> list = JsonRpcSerializer.fromJson(msg);
            for (JsonRpcBaseMessage elem : list) {
                count++;
                assertTrue(expectedClass.isInstance(elem));
            }
        }
        assertEquals(expectedCount, count);
    }

    @Test
    public void testRequestMessages() {
        String[] msgs = {
            "{\"jsonrpc\": \"2.0\", \"method\": \"subtract\", \"params\": [42, 23], \"id\": 1}",
            "{\"jsonrpc\": \"2.0\", \"method\": \"subtract\", \"params\": {\"subtrahend\": 23, \"minuend\": 42}"
                + ", \"id\": 2}",
            "{\"jsonrpc\": \"2.0\", \"method\": \"foobar\", \"id\": \"3\"}",
            "{\"jsonrpc\": \"2.0\", \"method\": \"update\", \"params\": [1,2,3,4,5], \"id\": 4}",
            "{\"jsonrpc\": \"2.0\", \"method\": \"foobar\", \"params\": [\"bar\", \"baz\"], \"id\": 5}",
            "{\"jsonrpc\": \"2.0\", \"method\": 1, \"params\": \"bar\", \"id\": 6}",
            "[{\"jsonrpc\": \"2.0\", \"method\": \"notify_sum\", \"params\": [1,2,4], \"id\": 7}, {\"jsonrpc\": \"2.0\""
                + ", \"method\": \"notify_hello\", \"params\": [7], \"id\": 8}]"
        };

        testMessagesHelper(msgs, 8, JsonRpcRequestMessage.class);
    }

    @Test
    public void testNotificationMessages() {
        String[] msgs = {
            "{\"jsonrpc\": \"2.0\", \"method\": \"subtract\", \"params\": [42, 23]}"
        };

        testMessagesHelper(msgs, 1, JsonRpcNotificationMessage.class);
    }

    @Test
    public void testReplyMessages() {
        String[] msgs = {
            "{\"jsonrpc\": \"2.0\", \"result\": 19, \"id\": 4}",
            "[{\"jsonrpc\": \"2.0\", \"result\": 7, \"id\": \"1\"},"
                + " {\"jsonrpc\": \"2.0\", \"result\": 19, \"id\": \"2\"},"
                + " {\"jsonrpc\": \"2.0\", \"error\": {\"code\": -32600, \"message\""
                    + ": \"Invalid Request\"}, \"id\": null},"
                + " {\"jsonrpc\": \"2.0\", \"error\": {\"code\": -32601, \"message\""
                    + ": \"Method not found\"}, \"id\": \"5\"},"
                + " {\"jsonrpc\": \"2.0\", \"result\": [\"hello\", 5], \"id\": \"9\"} ]",
            "{\"jsonrpc\": \"2.0\", \"error\": {\"code\": -32601, \"message\": \"Method not found\"}, \"id\": \"1\"}",
            "{\"jsonrpc\": \"2.0\", \"error\": {\"code\": -32700, \"message\": \"Parse error\"}, \"id\": null}",
            "[{\"jsonrpc\": \"2.0\", \"error\": {\"code\": -32600, \"message\": \"Invalid Request\"}, \"id\": null}]"
        };

        testMessagesHelper(msgs, 9, JsonRpcReplyMessage.class);
    }

    @Test
    public void testErrorMessages() {
        String[] msgs = {
            " {\"foo\": \"boo\"}",
            " {\"jsonrpc\": \"1.0\", \"method\": \"foo.get\", \"params\": {\"name\": \"myself\"}, \"id\": \"5\"}",
            " {\"jsonrpc\": \"2.0\", \"method\": \"get_data\", \"id\": \"9\""
        };

        testMessagesHelper(msgs, 3, JsonRpcMessageError.class);
    }

}
