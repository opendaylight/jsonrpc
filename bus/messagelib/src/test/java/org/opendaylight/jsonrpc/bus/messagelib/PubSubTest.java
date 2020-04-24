/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcNotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for PUB/SUB pattern. Only ZMQ and WS support this.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 27, 2018
 */
@RunWith(Parameterized.class)
public class PubSubTest {
    private static final Logger LOG = LoggerFactory.getLogger(PubSubTest.class);
    private MessageLibrary ml;
    private final String transport;

    @Parameters
    public static Collection<String[]> data() {
        return Lists.newArrayList(new String[] { "ws" }, new String[] { "zmq" });
    }

    public PubSubTest(final String transport) {
        this.transport = transport;
    }

    @Before
    public void setUp() {
        ml = new MessageLibrary(transport);
    }

    @After
    public void tearDown() {
        ml.close();
    }

    @Test
    public void test() throws Exception {
        final int subCount = 5;
        final int msgCount = 20;
        final int port = TestHelper.getFreeTcpPort();
        final CountDownLatch latch = new CountDownLatch(subCount * msgCount);
        final List<SubscriberSession> subscribers = new ArrayList<>(subCount);
        final PublisherSession pub = ml.publisher(TestHelper.getBindUri(transport, port), true);
        for (int i = 0; i < subCount; i++) {
            final SubscriberSession sub = ml.subscriber(TestHelper.getConnectUri(transport, port),
                    new NotificationMessageHandler() {
                        @Override
                        public void handleNotification(JsonRpcNotificationMessage notification) {
                            LOG.info("Notification : {}", notification);
                            latch.countDown();
                        }
                    }, true);
            sub.await();
            subscribers.add(sub);
        }
        for (int i = 0; i < msgCount; i++) {
            pub.publish("test", new JsonObject());
        }
        latch.await();
        pub.close();
    }
}
