/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage.Builder;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class MessageLibraryNegativeTest {
    private static final String BAD_JSON_STRING =
            "{\"jsonrpc\":\"2.0\",\"id\":100,\"method\":\"publish\",\"params\":\"zyxwuv\"";
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
        logger = LoggerFactory.getLogger(MessageLibraryNegativeTest.class);
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

    /*
     * Create a generic request message. Contents are not important.
     */
    private JsonRpcRequestMessage createRequestMessage(boolean isNotification) {
        String method = "method1";
        String msg = "zyxwuv";
        Builder builder = JsonRpcRequestMessage.builder().method(method).paramsFromObject(msg);
        if (!isNotification) {
            builder.idFromIntValue(100);
        }
        return builder.build();
    }

    /*
     * Create a generic reply message. Contents are not important.
     */
    private JsonRpcReplyMessage createReplyMessage() {
        return JsonRpcReplyMessage.builder().idFromIntValue(100).resultFromObject("zyxwuv").build();
    }

    /*
     * Test that publisher cannot receive message.
     */
    @Test(expected = MessageLibraryMismatchException.class)
    public void testPublisherReceive() throws MessageLibraryException {
        publisher.readMessage();
        fail("Exception not thrown");
    }

    /*
     * Test that subscriber cannot send message.
     */
    @Test(expected = MessageLibraryMismatchException.class)
    public void testSubscriberSend() throws MessageLibraryException {
        subscriber.sendMessage("Hello");
        fail("Exception not thrown");
    }

    /*
     * Test that a badly created JSON request would result in -32700 error from responder.
     */
    @Test
    public void testResponderReceiveBadJson() throws MessageLibraryException {
        client.sendMessage(BAD_JSON_STRING);
        server.handleIncomingMessage();
        client.handleIncomingMessage();
        logger.debug("Received error: {}", handler.error);
        assertNotNull(handler.error);
        assertEquals(-32700, handler.error.getCode());
    }

    /*
     * Test that a badly created JSON reply would throw exception.
     */
    @Test(expected = MessageLibraryMismatchException.class)
    public void testRequesterReceiveBadJson() throws MessageLibraryException {
        JsonRpcRequestMessage request = createRequestMessage(false);
        client.sendMessage(request);
        server.readMessage();
        server.sendMessage(BAD_JSON_STRING);
        client.handleIncomingMessage();
        fail("Exception not thrown");
    }

    /*
     * Test that a badly created JSON notification would throw exception.
     */
    @Test(expected = MessageLibraryMismatchException.class)
    public void testSubscriberReceiveBadJson() throws MessageLibraryException {
        publisher.sendMessage(BAD_JSON_STRING);
        subscriber.handleIncomingMessage();
        fail("Exception not thrown");
    }

    /*
     * Test that Responder cannot receive Reply messages.
     * Sends an error response back.
     */
    @Test
    public void testResponderReceiveReply() throws MessageLibraryException {
        JsonRpcReplyMessage reply = createReplyMessage();
        client.sendMessage(reply);
        server.handleIncomingMessage();
        client.handleIncomingMessage();
        logger.debug("Received error: {}", handler.error);
        assertNotNull(handler.error);
        assertEquals(-32600, handler.error.getCode());
    }

    /*
     * Test that Responder cannot receive Notification messages.
     * Sends an error response back.
     */
    @Test
    public void testResponderReceiveNotification() throws MessageLibraryException {
        JsonRpcRequestMessage notification = createRequestMessage(true);
        client.sendMessage(notification);
        server.handleIncomingMessage();
        client.handleIncomingMessage();
        logger.debug("Received error: {}", handler.error);
        assertNotNull(handler.error);
        assertEquals(-32600, handler.error.getCode());
    }

    /*
     * Test that Requester cannot receive Request messages.
     * Throws an exception.
     */
    @Test(expected = MessageLibraryMismatchException.class)
    public void testRequesterReceiveRequest() throws MessageLibraryException {
        JsonRpcRequestMessage request = createRequestMessage(false);
        client.sendMessage(request);
        server.readMessage();
        server.sendMessage(request);
        client.handleIncomingMessage();
    }

    /*
     * Test that Requester cannot receive Notification messages.
     * Throws an exception.
     */
    @Test(expected = MessageLibraryMismatchException.class)
    public void testRequesterReceiveNotification() throws MessageLibraryException {
        JsonRpcRequestMessage request = createRequestMessage(false);
        client.sendMessage(request);
        String rxMsg = server.readMessage();
        assertEquals(JsonRpcSerializer.toJson(request), rxMsg);
        JsonRpcRequestMessage notification = createRequestMessage(true);
        server.sendMessage(notification);
        client.handleIncomingMessage();
    }

    /*
     * Test that Subscriber cannot receive Request messages.
     * Throws an exception.
     */
    @Test(expected = MessageLibraryMismatchException.class)
    public void testSubscriberReceiveRequest() throws MessageLibraryException {
        JsonRpcRequestMessage request = createRequestMessage(false);
        publisher.sendMessage(request);
        subscriber.handleIncomingMessage();
    }

    /*
     * Test that Subscriber cannot receive Reply messages.
     * Throws an exception.
     */
    @Test(expected = MessageLibraryMismatchException.class)
    public void testSubscriberReceiveReply() throws MessageLibraryException {
        JsonRpcReplyMessage reply = createReplyMessage();
        publisher.sendMessage(reply);
        subscriber.handleIncomingMessage();
    }

    @AfterClass
    public static void teardown() {
        showFunctionName();
        client.close();
        server.close();
        subscriber.close();
        publisher.close();
        messaging.close();
    }
}
