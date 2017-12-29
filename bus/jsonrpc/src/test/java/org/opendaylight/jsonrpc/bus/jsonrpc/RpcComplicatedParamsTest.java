/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcComplicatedParamsTest {
    private static Logger logger;

    @BeforeClass
    public static void setup() {
        logger = LoggerFactory.getLogger(RpcComplicatedParamsTest.class);
    }

    private RpcComplicatedParams createParams() {
        String[] strArray = { "first", "second" };
        int[] intArray = { 100, 101 };
        RpcComplicatedParams param = new RpcComplicatedParams("abc", 200, strArray, intArray);
        logger.info(param.toString());
        return param;
    }

    @Test
    public void testSingleParam() {
        RpcComplicatedParams param1 = createParams();

        JsonRpcRequestMessage request1 = new JsonRpcRequestMessage();
        request1.setDefaultJsonrpc();
        request1.setMethod("echo");
        request1.setParamsAsObject(param1);

        String requestStr = JsonRpcSerializer.toJson(request1);
        logger.info(requestStr);

        List<JsonRpcBaseMessage> list = JsonRpcSerializer.fromJson(requestStr);
        assertEquals(1, list.size());
        JsonRpcBaseMessage elem = list.get(0);
        assertTrue(elem instanceof JsonRpcRequestMessage);

        JsonRpcRequestMessage request2 = (JsonRpcRequestMessage) elem;
        try {
            assertEquals("echo", request2.getMethod());
            RpcComplicatedParams param2 = request2.getParamsAsObject(RpcComplicatedParams.class);
            logger.info(param2.toString());
            assertEquals(param1, param2);
        } catch (JsonRpcException e) {
            logger.error("JSON message error", e);
            fail("Unexpected exception");
        }
    }

    @Test
    public void testMultipleParam() {
        RpcComplicatedParams complicated = createParams();
        Object[] param1 = { complicated, "alpha", 10, complicated, 20, "beta" };
        logger.info(Arrays.deepToString(param1));

        JsonRpcRequestMessage request1 = new JsonRpcRequestMessage();
        request1.setDefaultJsonrpc();
        request1.setMethod("echo");
        request1.setParamsAsObject(param1);

        String requestStr = JsonRpcSerializer.toJson(request1);
        logger.info(requestStr);

        List<JsonRpcBaseMessage> list = JsonRpcSerializer.fromJson(requestStr);
        assertEquals(1, list.size());
        JsonRpcBaseMessage elem = list.get(0);
        assertTrue(elem instanceof JsonRpcRequestMessage);

        JsonRpcRequestMessage request2 = (JsonRpcRequestMessage) elem;
        try {
            assertEquals("echo", request2.getMethod());
            RpcComplicatedParams c1 = request2.getParamsAtIndexAsObject(0, RpcComplicatedParams.class);
            String s1 = request2.getParamsAtIndexAsObject(1, String.class);
            int i1 = request2.getParamsAtIndexAsObject(2, int.class);
            RpcComplicatedParams c2 = request2.getParamsAtIndexAsObject(3, RpcComplicatedParams.class);
            int i2 = request2.getParamsAtIndexAsObject(4, int.class);
            String s2 = request2.getParamsAtIndexAsObject(5, String.class);

            Object[] param2 = { c1, s1, i1, c2, i2, s2 };
            logger.info(Arrays.deepToString(param2));
            assertArrayEquals(param1, param2);
        } catch (JsonRpcException e) {
            logger.error("JSON message error", e);
            fail("Unexpected exception");
        }
    }
}
