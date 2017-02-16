/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.BusSessionTimeoutException;
import org.opendaylight.jsonrpc.bus.zmq.ZMQFactory;
import org.opendaylight.jsonrpc.bus.zmq.ZMQSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZMQSessionParallelTest {
    private static ZMQFactory factory;
    private static ZMQSession rep;
    private static ZMQSession[] req;

    static Logger logger;
    String msg1 = "Hello";
    String msg2 = "World";
    String msg3 = "Book";
    String msg4 = "Shelf";
    String rxMsg;
    static int timeout = 500; // 0.5 second wait
    static int req_count = 10; // # of parallel requesters
    static int delay = 30; // ensure that: delay * req_count < timeout

    private static void showFunctionName() {
        logger.info(Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    @BeforeClass
    public static void setup() {
        logger = LoggerFactory.getLogger(ZMQSessionParallelTest.class);
        showFunctionName();

        factory = new ZMQFactory();
        assertNotNull(factory);

        // create responder
        String port = TestHelper.getFreeTcpPort();
        rep = factory.responder("tcp://*:" + port);
        assertNotNull(rep);
        rep.setTimeout(timeout);

        // create all requesters
        req = new ZMQSession[req_count];
        for (int i = 0; i < req_count; i++) {
            req[i] = factory.requester("tcp://127.0.0.1:" + port);
            assertNotNull(req[i]);
            req[i].setTimeout(timeout);
        }
    }

    @Test
    public void parallelReqRepSendReceive() throws BusSessionTimeoutException {
        showFunctionName();
        // Send messages via all requesters
        for (int i = 0; i < req_count; i++) {
            req[i].sendMessage(msg1);
        }

        // Process messages on responder
        for (int i = 0; i < req_count; i++) {
            rxMsg = rep.readMessage();
            assertEquals(msg1, rxMsg);
            try {
                // pretend that we are working
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                logger.debug("Sleep interrupted", e);
                Thread.currentThread().interrupt();
            }
            rep.sendMessage(msg2);
        }

        // Process replies on requesters
        for (int i = 0; i < req_count; i++) {
            rxMsg = req[i].readMessage();
            assertEquals(msg2, rxMsg);
        }
    }


    @AfterClass
    public static void teardown() {
        showFunctionName();
        rep.close();
        for (int i = 0; i < req_count; i++) {
            req[i].close();
        }
    }
}
