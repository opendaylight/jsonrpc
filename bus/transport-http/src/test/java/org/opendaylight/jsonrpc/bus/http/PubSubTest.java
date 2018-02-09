/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.PeerContext;
import org.opendaylight.jsonrpc.bus.api.Publisher;
import org.opendaylight.jsonrpc.bus.api.Subscriber;
import org.opendaylight.jsonrpc.bus.spi.AbstractSessionTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PubSubTest extends AbstractSessionTest {
    private static final Logger LOG = LoggerFactory.getLogger(PubSubTest.class);

    @Test
    public void testSmallMessage() throws InterruptedException {
        final int port = getFreeTcpPort();
        final CountDownLatch latch = new CountDownLatch(1);
        final Subscriber subscriber = factory.subscriber(getConnectUri(port), new MessageListener() {
            @Override
            public void onMessage(PeerContext peerContext, String message) {
                LOG.info("Response : {}", message);
                latch.countDown();
            }
        });
        final Publisher publisher = factory.publisher(getBindUri(port));
        subscriber.awaitConnection();
        publisher.publish("TEST");
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        publisher.close();
        subscriber.close();

    }

    @Test(timeout = 15_000)
    public void testMultipleSubscribers() throws InterruptedException {
        final int port = getFreeTcpPort();
        final int subCount = 10;
        final int msgs = 20;
        final CountDownLatch latch = new CountDownLatch(msgs * subCount);
        final List<Subscriber> subs = new ArrayList<>();
        final Publisher publisher = factory.publisher(getBindUri(port));
        for (int i = 0; i < subCount; i++) {
            final Subscriber subscriber = factory.subscriber(getConnectUri(port), new MessageListener() {
                @Override
                public void onMessage(PeerContext peerContext, String message) {
                    LOG.info("Messsage : {}", message);
                    latch.countDown();
                }
            });
            subscriber.awaitConnection();
            subs.add(subscriber);
        }
        for (int i = 0; i < msgs; i++) {
            publisher.publish("Ola!");
        }
        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }

    @Override
    protected BusSessionFactory createFactory() {
        return new WsBusSessionFactory(group, group, group);
    }
}
