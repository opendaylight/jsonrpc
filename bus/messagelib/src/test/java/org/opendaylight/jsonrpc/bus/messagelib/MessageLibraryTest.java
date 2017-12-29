/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageLibraryTest {
    private static MessageLibrary messaging;
    private static TestMessageHandler handler;
    private static Logger logger;
    private static Session client;
    private static Session server;
    private static Session publisher;
    private static Session subscriber;

    private static void showFunctionName() {
        logger.info(Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    @BeforeClass
    public static void setup() {
        logger = LoggerFactory.getLogger(MessageLibraryTest.class);
        showFunctionName();

        messaging = new MessageLibrary("zmq");
        String port1 = TestHelper.getFreeTcpPort();
        server = messaging.responder("tcp://*:" + port1);
        client = messaging.requester("tcp://localhost:" + port1);

        String port2 = TestHelper.getFreeTcpPort();
        publisher = messaging.publisher("tcp://*:" + port2);
        subscriber = messaging.subscriber("tcp://localhost:" + port2);

        handler = new TestMessageHandler();
        server.setRequestMessageHandler(handler);
        client.setReplyMessageHandler(handler);
        subscriber.setNotificationMessageHandler(handler);

        try {
            // Let subscriber join. Sleep is the best method we have for now.
            Thread.sleep(200);
        } catch (InterruptedException e) {
            logger.debug("Interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testServerClientEcho() throws MessageLibraryException {
        showFunctionName();
        client.sendRequest("echo", "abc");
        int serverCount = server.handleIncomingMessage();
        int clientCount = client.handleIncomingMessage();
        assertEquals("abc", handler.result);
        assertEquals(1, serverCount);
        assertEquals(1, clientCount);
    }

    @Test
    public void testServerClientConcat() throws MessageLibraryException {
        showFunctionName();
        // Create params to send on request channel
        Object[] params = { "first", "second" };
        client.sendRequest("concat", params);
        int serverCount = server.handleIncomingMessage();
        int clientCount = client.handleIncomingMessage();
        assertEquals("firstsecond", handler.result);
        assertEquals(1, serverCount);
        assertEquals(1, clientCount);
    }

    @Test
    public void testServerClientInvalidMethod() throws MessageLibraryException {
        showFunctionName();
        // Create method and params and send on request channel
        client.sendRequest("dummy", null);
        final int serverCount = server.handleIncomingMessage();
        final int clientCount = client.handleIncomingMessage();
        assertEquals(-32601, handler.error.getCode());
        assertEquals(null, handler.result);
        assertEquals(1, serverCount);
        assertEquals(1, clientCount);
    }

    @Test
    public void testServerClientInvalidMessage() throws MessageLibraryException {
        showFunctionName();
        // Create method and params and send on request channel
        JsonRpcRequestMessage msg = new JsonRpcRequestMessage();
        msg.setMethod("dummy");
        msg.setIdAsIntValue(4);
        client.sendMessage(msg);
        final int serverCount = server.handleIncomingMessage();
        final int clientCount = client.handleIncomingMessage();
        assertEquals(-32700, handler.error.getCode());
        assertEquals(null, handler.result);
        assertEquals(1, serverCount);
        assertEquals(1, clientCount);
    }

    @Test
    public void testServerClientInvalidMethodParameters() throws MessageLibraryException {
        showFunctionName();
        String[] params = { "any-non-integer-argument" };
        client.sendRequest("increment", params);
        server.handleIncomingMessage();
        client.handleIncomingMessage();
        assertEquals(-32602, handler.error.getCode());
    }

    @Test(timeout = 600)
    public void testSessionTimeout() throws MessageLibraryException {
        showFunctionName();
        final int defaultTime = client.getTimeout();
        client.setTimeout(200);
        assertEquals(200, client.getTimeout());

        // Create params to send on request channel
        int param = 300;
        client.sendRequest("delayedResponse", param);
        int clientCount = client.handleIncomingMessage();
        assertEquals(0, clientCount); // There should be no message
        // clear server's channel for other tests
        int serverCount = server.handleIncomingMessage();
        assertEquals(1, serverCount);

        client.setTimeoutToDefault();
        assertEquals(defaultTime, client.getTimeout());
    }

    @Test
    public void testPubSub() throws MessageLibraryException {
        showFunctionName();
        publisher.sendRequest("noticeMethod", "noticeParam");
        int subscriberCount = subscriber.handleIncomingMessage();
        assertEquals("noticeMethod", handler.noticeMethod);
        assertEquals("noticeParam", handler.noticeParam);
        assertEquals(1, subscriberCount);
    }

    @AfterClass
    public static void teardown() {
        showFunctionName();
        messaging.close();
    }
}
