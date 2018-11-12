/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.jsonrpc.bus.api.Requester;
import org.opendaylight.jsonrpc.bus.api.Responder;
import org.opendaylight.jsonrpc.bus.spi.AbstractSessionTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractReqRepTest extends AbstractSessionTest {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractReqRepTest.class);

    protected void testReqRep(String reqUri, String repUri, String payload, String expectedResponse)
            throws InterruptedException, ExecutionException, TimeoutException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Responder responder = factory.responder(repUri, (peerContext, message) -> {
            LOG.info("Received request {}", message);
            assertEquals(payload, message);
            peerContext.send(expectedResponse);
            latch.countDown();
        });
        final Requester requester = factory.requester(reqUri,
            (peerContext, message) -> LOG.info("Received response {}", message));
        requester.awaitConnection();
        final String response = requester.send(payload).get(30, TimeUnit.SECONDS);
        LOG.info("Received response {}", response);
        assertEquals(expectedResponse, response);
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        requester.close();
        responder.close();
    }
}
