/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import static org.junit.Assert.assertTrue;

import com.google.common.base.Strings;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.PeerContext;
import org.opendaylight.jsonrpc.bus.api.RecoverableTransportException;
import org.opendaylight.jsonrpc.bus.api.Requester;
import org.opendaylight.jsonrpc.bus.api.Responder;
import org.opendaylight.jsonrpc.bus.spi.AbstractSessionTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReqRepTest extends AbstractSessionTest {
    private static final Logger LOG = LoggerFactory.getLogger(ReqRepTest.class);

    @Test(timeout = 15_000)
    public void testShortFrame() throws Exception {
        final int port = getFreeTcpPort();
        final CountDownLatch latch = new CountDownLatch(2);
        final Responder responder = factory.responder(getBindUri(port), (peerContext, message) -> {
            LOG.info("Received request : '{}'", message);
            // echo back
            peerContext.send(message);
            if ("Hi".equals(message)) {
                latch.countDown();
            }
        });
        final Requester requester = factory.requester(getConnectUri(port), new MessageListener() {
            @Override
            public void onMessage(PeerContext peerContext, String message) {
                LOG.info("Received response : {}", message);
                if ("Hi".equals(message)) {
                    latch.countDown();
                }
            }
        });
        requester.awaitConnection();
        requester.send("Hi");
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        responder.close();
        requester.close();
    }

    @Test(timeout = 15_000)
    public void testLongFrame() throws Exception {
        final int port = getFreeTcpPort();
        final CountDownLatch latch = new CountDownLatch(2);
        final String msg = Strings.repeat("X", 256);
        final Responder responder = factory.responder(getBindUri(port), (peerContext, message) -> {
            if (msg.equals(message)) {
                // echo back
                peerContext.send(message);
                latch.countDown();
            }
        });
        final Requester requester = factory.requester(getConnectUri(port), new MessageListener() {
            @Override
            public void onMessage(PeerContext peerContext, String message) {
                LOG.info("Received response : {}", message);
                if (msg.equals(message)) {
                    latch.countDown();
                }
            }
        });
        requester.awaitConnection();
        requester.send(msg);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        responder.close();
        requester.close();
    }

    @Test(expected = RecoverableTransportException.class)
    public void testConnectionFailed() throws InterruptedException, ExecutionException, TimeoutException {
        final int port = getFreeTcpPort();
        final Requester requester = factory.requester(getConnectUri(port),
            (peerContext, message) -> LOG.info("Received response {}", message));
        requester.send("").get(1, TimeUnit.SECONDS);
    }

    @Override
    protected BusSessionFactory createFactory() {
        return new ZmqBusSessionFactory(config);
    }
}
