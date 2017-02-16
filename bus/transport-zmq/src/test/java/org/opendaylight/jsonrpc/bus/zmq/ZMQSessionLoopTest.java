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
import org.opendaylight.jsonrpc.bus.BusSessionMsgHandler;
import org.opendaylight.jsonrpc.bus.BusSessionTimeoutException;
import org.opendaylight.jsonrpc.bus.zmq.ZMQFactory;
import org.opendaylight.jsonrpc.bus.zmq.ZMQSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZMQSessionLoopTest {
    private static ZMQFactory factory;
    private static ZMQSession rep;
    private static ZMQSession req;

    static Logger logger;
    String msg1 = "Hello";
    String rxMsg;

    private class ServerHandler implements BusSessionMsgHandler, Runnable
    {
        private ZMQSession session;
        
        public ServerHandler(ZMQSession session) {
            this.session = session;
        }
        
        // Echo the message. Close for "close" message.
        @Override
        public int handleIncomingMsg(String message) {
            session.sendMessage(message);
            return (message.equals("close")) ? -1 : 0;
        }

        @Override
        public void run() {
            session.startLoop(this);
        }
    }

    private static void showFunctionName() {
        logger.info(Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    @BeforeClass
    public static void setup() {
        logger = LoggerFactory.getLogger(ZMQSessionLoopTest.class);
        showFunctionName();

        factory = new ZMQFactory();
        assertNotNull(factory);

        String port = TestHelper.getFreeTcpPort();
        rep = factory.responder("tcp://*:" + port);
        assertNotNull(rep);
        
        req = factory.requester("tcp://127.0.0.1:" + port);
        assertNotNull(req);
    }

    @Test
    public void testReqLoopExternalStop() throws BusSessionTimeoutException, InterruptedException {
        showFunctionName();
        ServerHandler handler = new ServerHandler(rep);
        Thread serverThread = new Thread(handler);
        serverThread.start();
        req.sendMessage(msg1);
        rxMsg = req.readMessage();
        assertEquals(msg1, rxMsg);
        // Stop the loop using external request.
        rep.stopLoop();
        serverThread.join();
    }


    @Test
    public void testReqLoopInternalStop() throws BusSessionTimeoutException, InterruptedException {
        showFunctionName();
        ServerHandler handler = new ServerHandler(rep);
        Thread serverThread = new Thread(handler);
        serverThread.start();
        req.sendMessage(msg1);
        rxMsg = req.readMessage();
        assertEquals(msg1, rxMsg);
        // Send request to server to close itself.
        req.sendMessage("close");
        rxMsg = req.readMessage();
        assertEquals("close", rxMsg);
        serverThread.join();
    }

    @AfterClass
    public static void teardown() {
        showFunctionName();
        rep.close();
        req.close();
        factory.close();
    }

}
