/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.BusSessionTimeoutException;
import org.opendaylight.jsonrpc.bus.zmq.ZMQFactory;
import org.opendaylight.jsonrpc.bus.zmq.ZMQSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

public class ZMQSessionTest {
    private static ZMQFactory factory;
    private static ZMQSession rep;
    private static ZMQSession req;
    private static ZMQSession pub;
    private static ZMQSession sub;

    static Logger logger;
    String msg1 = "Hello";
    String msg2 = "World";
    String msg3 = "Book";
    String msg4 = "Shelf";
    String rxMsg;
    static int timeout = 500; // 0.5 second wait

    private static void showFunctionName() {
        logger.info(Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    @BeforeClass
    public static void setup() {
        logger = LoggerFactory.getLogger(ZMQSessionTest.class);
        showFunctionName();

        factory = new ZMQFactory();
        assertNotNull(factory);

        String port1 = TestHelper.getFreeTcpPort();
        rep = factory.responder("tcp://*:" + port1);
        assertNotNull(rep);
        rep.setTimeout(timeout);
        
        req = factory.requester("tcp://127.0.0.1:" + port1);
        assertNotNull(req);
        req.setTimeout(timeout);

        String port2 = TestHelper.getFreeTcpPort();
        pub = factory.publisher("tcp://*:" + port2);
        assertNotNull(pub);
        pub.setTimeout(timeout);

        sub = factory.subscriber("tcp://127.0.0.1:" + port2);
        assertNotNull(sub);
        sub.setTimeout(timeout);
    }

    @Test
    public void testSocketTypes() {
        showFunctionName();
        assertEquals(ZMQ.REP, rep.getSocketType());
        assertEquals(ZMQ.REQ, req.getSocketType());
        assertEquals(ZMQ.PUB, pub.getSocketType());
        assertEquals(ZMQ.SUB, sub.getSocketType());
    }

    @Test
    public void testGetTimeout() {
        showFunctionName();
        assertEquals(timeout, rep.getTimeout());
        assertEquals(timeout, req.getTimeout());
        assertEquals(timeout, pub.getTimeout());
        assertEquals(timeout, sub.getTimeout());
    }

    @Test
    public void reqRepSendReceive() throws BusSessionTimeoutException {
        showFunctionName();
        req.sendMessage(msg1);
        rxMsg = rep.readMessage();
        assertEquals(msg1, rxMsg);

        rep.sendMessage(msg2);
        rxMsg = req.readMessage();
        assertEquals(msg2, rxMsg);
    }

    @Test
    public void pubSubSendReceive() throws InterruptedException, BusSessionTimeoutException 
    {
        showFunctionName();
        Thread.sleep(200); // waiting for sub to actually join
        pub.sendMessage(msg1);
        rxMsg = sub.readMessage();
        assertEquals(msg1, rxMsg);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void subSend() {
        showFunctionName();
        boolean result = sub.sendMessage(msg1);
        assertFalse(result);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void pubReceieve() throws BusSessionTimeoutException {
        showFunctionName();
        rxMsg = pub.readMessage();
        assertNull(rxMsg);
    }

    @Test(expected=BusSessionTimeoutException.class)
    public void reqReceiveTimeout() throws BusSessionTimeoutException {
        showFunctionName();
        rxMsg = req.readMessage(); // no client message has been sent.
        assertNull(rxMsg);
    }

    @Test
    public void reqDoubleSend() throws BusSessionTimeoutException {
        showFunctionName();
        boolean result;
        result = req.sendMessage(msg2);
        assertTrue(result);
        result = req.sendMessage(msg1);
        assertFalse(result);

        // cleanup the messages on req-rep channel so that
        // they can be used by other tests 
        rxMsg = rep.readMessage();
        rep.sendMessage(rxMsg);
        rxMsg = req.readMessage();
    }

    @Test(timeout=600)
    public void reqManualReopen() throws BusSessionTimeoutException {
        showFunctionName();
        boolean result;

        // Ensure req-rep still works after requester has reopened.
        result = req.sendMessage(msg1);
        assertTrue(result);
        rxMsg = rep.readMessage();
        assertEquals(msg1, rxMsg);
        req.reopen();
        rep.sendMessage(msg2);
        //No need to read msg2, as req has restarted

        result = req.sendMessage(msg3);
        assertTrue(result);
        rxMsg = rep.readMessage();
        assertEquals(rxMsg, msg3);
        rep.sendMessage(msg4);
        rxMsg = req.readMessage();
        assertEquals(msg4, rxMsg);
    }

    @Test(timeout=600)
    public void reqTimeoutReopen() throws BusSessionTimeoutException {
        showFunctionName();
        boolean result;

        // First try receive after requester has reopened.
        result = req.sendMessage(msg1);
        assertTrue(result);
        rxMsg = rep.readMessage();
        assertEquals(rxMsg, msg1);
        // rep never replies and req should timeout
        try {
            rxMsg = req.readMessage();
        } catch (BusSessionTimeoutException e) {
            assertThat(e.getMessage(), containsString("Receive timed out"));
        }
        rep.sendMessage(msg2); // send msg to clear channel

        // Ensure req-rep still works
        result = req.sendMessage(msg3);
        assertTrue(result);
        rxMsg = rep.readMessage();
        assertEquals(rxMsg, msg3);
        rep.sendMessage(msg4);
        rxMsg = req.readMessage();
        assertEquals(msg4, rxMsg);
    }

    @Test // should not throw
    public void testUriForgiveness() {
        factory.responder("tcp://*:12345");  // happy uri
        factory.responder("tcp://*:12346/"); // sloppy, but acceptable uri
    }

    @AfterClass
    public static void teardown() {
        showFunctionName();
        rep.close();
        req.close();
        pub.close();
        sub.close();
    }

}
