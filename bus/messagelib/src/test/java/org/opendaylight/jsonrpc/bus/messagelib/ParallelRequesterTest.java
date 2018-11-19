/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParallelRequesterTest {
    private static Logger logger;
    private static MessageLibrary messaging;
    private static ProxyServiceImpl proxy;
    private static ThreadedSession server;
    private static String port;
    private static int timeout = 150;
    private static int echoDelay = 100;
    private static int threadCount = 50;
    private CountDownLatch goodCount;

    private static void showFunctionName() {
        logger.info(Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    private class ParallelClient implements Runnable {
        private final ServerInterface serverProxy;
        private final String num;
        private final int delay;

        ParallelClient(ServerInterface serverProxy, int num, int delay) {
            this.serverProxy = serverProxy;
            this.num = Integer.toString(num);
            this.delay = delay;
        }

        @Override
        public void run() {
            String msg = "Hello " + num;
            logger.info("Started " + num);
            try {
                String rxMsg = serverProxy.delayedEcho(msg, delay);
                logger.info(rxMsg);
                if (rxMsg.equals(msg)) {
                    goodCount.countDown();
                } else {
                    logger.error("Receive string mismatch");
                }
            } catch (ProxyServiceTimeoutException e) {
                logger.error("Timeout while waiting for server response", e);
            }
            logger.info("Finished " + num);
        }
    }

    @BeforeClass
    public static void setupBeforeClass() throws ProxyServiceGenericException {
        logger = LoggerFactory.getLogger(ParallelRequesterTest.class);
        showFunctionName();
        messaging = new MessageLibrary("zmq");
        proxy = new ProxyServiceImpl(messaging);

        // Create new server thread
        port = TestHelper.getFreeTcpPort();
        server = messaging.threadedResponder("tcp://*:" + port, new TestMessageServer());
    }

    private void doTest(Thread[] clients) throws InterruptedException {
        goodCount = new CountDownLatch(clients.length);

        for (Thread client : clients) {
            client.start();
        }

        for (Thread client : clients) {
            try {
                client.join();
            } catch (InterruptedException e) {
                logger.error("Interrupted waiting for client join", e);
                Thread.currentThread().interrupt();
            }
        }

        // Verify that the message was validated properly in every thread.
        assertTrue(goodCount.await(15_000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testMultiClientSharedProxy() throws InterruptedException {
        // Create multiple clients with each sharing the server proxy.
        // Since these block on the client side, having a low timeout is fine.
        Thread[] clients = new Thread[threadCount];
        ServerInterface serverProxy = proxy.createRequesterProxy(
                "tcp://127.0.0.1:" + port, ServerInterface.class, timeout);

        for (int i = 0; i < threadCount; i++) {
            clients[i] = new Thread(new ParallelClient(serverProxy, i, echoDelay));
        }

        doTest(clients);
        serverProxy.close();
    }

    @Test
    public void testMultiClientSeparateProxy() throws InterruptedException {
        // Create multiple threads with each having a separate server proxy.
        // Since these do not block on client side, the timeout should be proportional
        // to the number of threads.
        Thread[] clients = new Thread[threadCount];
        ServerInterface[] servers = new ServerInterface[threadCount];

        for (int i = 0; i < threadCount; i++) {
            servers[i] = proxy.createRequesterProxy(
                        "tcp://127.0.0.1:" + port, ServerInterface.class, timeout * threadCount);
            clients[i] = new Thread(new ParallelClient(servers[i], i, echoDelay));
        }

        doTest(clients);

        for (ServerInterface server2 : servers) {
            server2.close();
        }
    }

    @AfterClass
    public static void teardown() throws InterruptedException, ExecutionException, TimeoutException {
        showFunctionName();
        server.stop().get();
        messaging.close();
    }
}
