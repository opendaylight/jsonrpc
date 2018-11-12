/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;

import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ProxyTest {
    private MessageLibrary ml;
    private ProxyServiceImpl svc;
    private TransportFactory tf;

    @Before
    public void setUp() {
        ml = new MessageLibrary("zmq");
        svc = new ProxyServiceImpl(ml);
        tf = new DefaultTransportFactory();
    }

    @After
    public void tearDown() throws Exception {
        ml.close();
        tf.close();
    }

    @Test(timeout = 15_000)
    public void testSubscriberProxy() throws Exception {
        final int port = TestHelper.getFreeTcpPort();
        final CountDownLatch latch = new CountDownLatch(1);
        final PublishInterface proxy = svc.createPublisherProxy(TestHelper.getBindUri("zmq", port),
                PublishInterface.class);

        final SubscriberSession subs = ml.subscriber(TestHelper.getConnectUri("zmq", port), notification -> {
            assertEquals("ABCD", ((JsonArray) notification.getParams()).get(0).getAsString());
            assertEquals(null, notification.getId());
            assertEquals("publish", notification.getMethod());
            latch.countDown();
        }, true);
        subs.await();
        proxy.publish("ABCD");
        latch.await(5, TimeUnit.SECONDS);
        proxy.close();
        subs.close();
    }

    @Test(timeout = 15_000, expected = ProxyServiceGenericException.class)
    public void testSubscriberProxyInvalid() throws Exception {
        final int port = TestHelper.getFreeTcpPort();
        final PublishExtraInterface proxy = svc.createPublisherProxy(TestHelper.getBindUri("zmq", port),
                PublishExtraInterface.class);

        final SubscriberSession subs = ml.subscriber(TestHelper.getConnectUri("zmq", port), notification -> {
        }, true);
        subs.await();
        try {
            proxy.invalidPublish("ABCD");
        } finally {
            proxy.close();
            subs.close();
        }
    }

    private void testRequesterProxy(String transport) throws InterruptedException {
        final int port = TestHelper.getFreeTcpPort();
        final ResponderSession resp = ml.responder(TestHelper.getBindUri(transport, port),
                new ResponderHandlerAdapter(new TestMessageServer()), true);
        ServerPartialInterface api = svc.createRequesterProxy(TestHelper.getConnectUri(transport, port),
                ServerPartialInterface.class);
        TimeUnit.MILLISECONDS.sleep(150);
        assertEquals("ABCXYZ", api.concat("ABC", "XYZ"));
        api.close();
        resp.close();
    }

    @Test(timeout = 15_000)
    public void testRequesterProxyZmq() throws InterruptedException {
        testRequesterProxy("zmq");
    }

    @Test(timeout = 15_000)
    public void testRequesterProxyWs() throws InterruptedException {
        testRequesterProxy("ws");
    }

    @Test(timeout = 15_000)
    public void testRequesterProxyHttp() throws InterruptedException {
        testRequesterProxy("http");
    }

    @Test(timeout = 1500_000)
    public void testPublisherProxy() throws URISyntaxException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final int port = TestHelper.getFreeTcpPort();
        PublishInterface proxy = tf.createPublisherProxy(PublishInterface.class, TestHelper.getBindUri("zmq", port));
        SubscriberSession session = tf.createSubscriber(TestHelper.getConnectUri("zmq", port), new PublishInterface() {

            @Override
            public void close() throws Exception {

            }

            @Override
            public void publish(String msg) {
                latch.countDown();
            }
        });
        session.await();
        proxy.publish("XYZ");
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        session.close();
    }
}
