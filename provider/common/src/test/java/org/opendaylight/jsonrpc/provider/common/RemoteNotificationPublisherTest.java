/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import static org.junit.Assert.assertThrows;
import static org.opendaylight.jsonrpc.provider.common.Util.findNode;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.messagelib.DefaultTransportFactory;
import org.opendaylight.jsonrpc.impl.RemoteControl;
import org.opendaylight.jsonrpc.model.RemoteNotificationPublisher;
import org.opendaylight.mdsal.dom.api.DOMNotification;
import org.opendaylight.mdsal.dom.api.DOMNotificationListener;
import org.opendaylight.mdsal.dom.broker.RouterDOMNotificationService;
import org.opendaylight.mdsal.dom.broker.RouterDOMPublishNotificationService;
import org.opendaylight.mdsal.dom.broker.RouterDOMRpcService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
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
    private Registration reg;
    private CountDownLatch latch;

    @Before
    public void setUp() {
        transportFactory = new DefaultTransportFactory();
        ctrl = new RemoteControl(getDomBroker(), schemaContext, transportFactory,
            new RouterDOMPublishNotificationService(getDOMNotificationRouter()),
            new RouterDOMRpcService(getDOMRpcRouter()), codecFactory);
        NotificationDefinition path = findNode(schemaContext, "test-model-notification:notification1",
                Module::getNotifications).orElseThrow();
        latch = new CountDownLatch(1);
        reg = new RouterDOMNotificationService(getDOMNotificationRouter())
            .registerNotificationListener(new DOMNotificationListener() {
                @Override
                public void onNotification(DOMNotification notification) {
                    LOG.info("Got notification : {}", notification);
                    latch.countDown();
                }
            }, Absolute.of(path.getQName()));
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
        ctrl.publishNotification("test-model-notification:notification1", data);
        latch.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void testWithoutPrefix() throws InterruptedException {
        JsonObject data = new JsonObject();
        ctrl.publishNotification("notification1", data);
        latch.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void testNonExistingModule() {
        assertThrows(IllegalArgumentException.class,
            () -> ctrl.publishNotification("module-not-exists:notification1", new JsonObject()));
    }

    @Test
    public void testNonExistingNotification() {
        assertThrows(IllegalArgumentException.class,
            () -> ctrl.publishNotification("notification-not-exists", new JsonObject()));
    }
}
