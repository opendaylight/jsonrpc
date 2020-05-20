/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static org.opendaylight.jsonrpc.provider.common.Util.findNode;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.messagelib.DefaultTransportFactory;
import org.opendaylight.jsonrpc.model.RemoteNotificationPublisher;
import org.opendaylight.mdsal.dom.api.DOMNotification;
import org.opendaylight.mdsal.dom.api.DOMNotificationListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link RemoteNotificationPublisher} part of {@link RemoteControl}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Apr 28, 2019
 */
public class RemoteNotificationPublisherTest extends AbstractJsonRpcTest {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteNotificationPublisherTest.class);
    private DefaultTransportFactory transportFactory;
    private RemoteControl ctrl;
    private ListenerRegistration<DOMNotificationListener> reg;
    private CountDownLatch latch;

    @Before
    public void setUp() {
        transportFactory = new DefaultTransportFactory();
        ctrl = new RemoteControl(getDomBroker(), schemaContext, transportFactory, getDOMNotificationRouter(),
                getDOMRpcRouter().getRpcService());
        NotificationDefinition path = findNode(schemaContext, "test-model:too-many-numbers", Module::getNotifications)
                .get();
        latch = new CountDownLatch(1);
        reg = getDOMNotificationRouter().registerNotificationListener(new DOMNotificationListener() {
            @Override
            public void onNotification(@NonNull DOMNotification notification) {
                LOG.info("Got notification : {}", notification);
                latch.countDown();
            }
        }, path.getPath());
        logTestName("START");
    }

    @After
    public void tearDown() {
        transportFactory.close();
        ctrl.close();
        reg.close();
        logTestName("END");
    }

    @Test
    public void testWithPrefix() throws InterruptedException {
        JsonObject data = new JsonObject();
        data.add("current-level", new JsonPrimitive(10));
        ctrl.publishNotification("test-model:too-many-numbers", data);
        latch.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void testWithoutPrefix() throws InterruptedException {
        JsonObject data = new JsonObject();
        ctrl.publishNotification("too-many-numbers", data);
        latch.await(10, TimeUnit.SECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonExistingModule() throws InterruptedException {
        ctrl.publishNotification("module-not-exists:too-many-numbers", new JsonObject());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonExistingNotification() throws InterruptedException {
        ctrl.publishNotification("notification-not-exists", new JsonObject());
    }
}
