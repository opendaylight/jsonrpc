/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyServiceRequesterTest {
    private static Logger logger;
    private static MessageLibrary messaging;
    private static ProxyServiceImpl proxy;
    private static ServerInterface serverProxy;
    private static ThreadedSession server;
    private static String port;
    private static int timeout = 500;

    private static void showFunctionName() {
        logger.info(Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    @BeforeClass
    public static void setupBeforeClass() throws ProxyServiceGenericException {
        logger = LoggerFactory.getLogger(ProxyServiceRequesterTest.class);
        showFunctionName();
        messaging = new MessageLibrary("zmq");

        // Create new server thread
        port = TestHelper.getFreeTcpPort();
        server = messaging.threadedResponder("tcp://*:" + port, new TestMessageServer());

        proxy = new ProxyServiceImpl(messaging);
        serverProxy = proxy.createRequesterProxy("tcp://127.0.0.1:" + port, ServerInterface.class,
                timeout);
    }

    @Test
    public void testProxyServerEcho() {
        showFunctionName();
        String result = serverProxy.echo("abc");
        assertEquals("abc", result);
    }

    @Test
    public void testProxyServerConcat() {
        showFunctionName();
        String result = serverProxy.concat("first", "second");
        assertEquals("firstsecond", result);
    }

    @Test
    public void testProxyServerJoin() {
        showFunctionName();
        String[] params = { "a", "b", "c", "d" };
        String result = serverProxy.join(",", params);
        assertEquals("a,b,c,d", result);
    }

    @Test(expected = ProxyServiceGenericException.class)
    public void testProxyServerInvalidFunction() {
        showFunctionName();
        String[] params = { "a", "b", "c", "d" };
        serverProxy.notImplemented(params);
        fail("Exception was not thrown");
    }

    @Test
    public void testProxyServerErrorReturn() throws Exception {
        showFunctionName();
        try {
            serverProxy.returnError(0);
        } catch (ProxyServiceGenericException e) {
            // Verify that we get standard error string in the message back
            assertTrue(e.getMessage().contains(JsonRpcErrorObject.JSONRPC_ERROR_MESSAGE_INTERNAL));
            return;
        }
        fail("Exception was not thrown");
    }

    @Test
    public void testProxyServerNoReturn() {
        showFunctionName();
        String params = "a";
        serverProxy.noReturn(params);
        // send a second request to ensure that channel is in working condition.
        String value = serverProxy.echo(params);
        assertEquals(params, value);
    }

    @Test
    public void testProxySecondClientJoin() throws ProxyServiceGenericException {
        showFunctionName();
        ServerPartialInterface serverProxy2 = proxy
                .createRequesterProxy("tcp://127.0.0.1:" + port, ServerPartialInterface.class);
        String[] params = { "a", "b", "c", "d" };
        String result = serverProxy2.join(",", params);
        assertEquals("a,b,c,d", result);
        serverProxy2.close();
    }

    @Test(timeout = 600)
    public void testServerTimeout() {
        showFunctionName();
        // verify timeout set at create works
        assertEquals(timeout, proxy.getTimeout(serverProxy));

        // test setting of new proxy
        proxy.setTimeout(serverProxy, 200);
        assertEquals(200, proxy.getTimeout(serverProxy));

        try {
            // Test that Timeout Exception is thrown
            serverProxy.delayedEcho("ABC", 300);
            fail("Expected an ProxyServiceTimeoutException to be thrown");
        } catch (ProxyServiceTimeoutException e) {
            assertTrue(true);
        }

        String msg = serverProxy.echo("Hello");
        assertEquals("Hello", msg);

        // restore timeout
        proxy.setTimeout(serverProxy, timeout);
    }

    @AfterClass
    public static void teardown() {
        showFunctionName();
        server.stop();
        server.joinAndClose();
        serverProxy.close();
        messaging.close();
    }
}
