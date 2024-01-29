/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.messagelib.DefaultTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.MessageLibrary;
import org.opendaylight.jsonrpc.bus.messagelib.PublisherSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumHashMap;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.hmap.JsonPathCodec;
import org.opendaylight.jsonrpc.impl.JsonRPCNotificationService;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.mdsal.dom.api.DOMNotificationListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.NotificationEndpointsBuilder;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link JsonRPCNotificationService}.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class JsonRPCNotificationServiceTest extends AbstractJsonRpcTest {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCNotificationServiceTest.class);
    private int port;
    private JsonRPCNotificationService svc;
    private RemoteGovernance governance;
    private PublisherSession pubSession;
    private Module mod;
    private MessageLibrary ml;
    private TransportFactory transportFactory;
    private final HierarchicalEnumMap<JsonElement, DataType, String> pathMap = HierarchicalEnumHashMap
            .create(DataType.class, JsonPathCodec.create());

    @Before
    public void setUp() throws Exception {
        port = getFreeTcpPort();
        governance = mock(RemoteGovernance.class);
        when(governance.governance(anyInt(), anyString(), any())).thenReturn(getPath());
        mod = schemaContext.findModule("test-model-notification", Revision.of("2020-10-14")).orElseThrow();

        transportFactory = new DefaultTransportFactory();
        svc = new JsonRPCNotificationService(getPeer(),
                new BuiltinSchemaContextProvider(schemaContext).createSchemaContext(getPeer()), pathMap,
                new JsonRpcCodecFactory(schemaContext), transportFactory, governance);
        ml = new MessageLibrary("ws");
        pubSession = ml.publisher(getPath(), true);
        TimeUnit.MILLISECONDS.sleep(150);
    }

    @After
    public void tearDown() {
        pubSession.close();
        svc.close();
        ml.close();
    }

    @Test
    public void testMultiple() throws InterruptedException {
        final int listeners = 10;
        final int count = 10;
        final CountDownLatch cl = new CountDownLatch(listeners * count);
        final Set<Registration> regs = new HashSet<>();
        for (int i = 0; i < listeners; i++) {
            regs.add(svc.registerNotificationListener((DOMNotificationListener) notification -> {
                LOG.info("Received notification : {}", notification);
                cl.countDown();
            }, notificationPath(mod, "notification1")));
        }
        TimeUnit.MILLISECONDS.sleep(5000L);
        for (int i = 0; i < count; i++) {
            pubSession.publish("notification1", new int[] { 1, 2 });
        }
        assertTrue(cl.await(5, TimeUnit.SECONDS));
        regs.stream().forEach(Registration::close);
    }

    @Test
    public void test() throws URISyntaxException, Exception {
        final CountDownLatch cl = new CountDownLatch(4);
        svc.registerNotificationListener((DOMNotificationListener) notification -> {
            LOG.info("Received notification : {}", notification);
            cl.countDown();
        }, notificationPath(mod, "notification1"));
        TimeUnit.MILLISECONDS.sleep(500L);
        // send primitive value - this is against specification, but we can
        // handle it easily
        pubSession.publish("notification1", 1);
        JsonObject obj = new JsonObject();
        obj.addProperty("current-level", 1);
        obj.addProperty("max-level", 2);
        // named parameters
        pubSession.publish("notification1", obj);
        // positional parameters, but not all values are present
        pubSession.publish("notification1", new int[] { 1, 3 });
        pubSession.publish("notification1", new int[] { 1, 2 });
        assertTrue(cl.await(5, TimeUnit.SECONDS));
    }

    private String getPath() {
        return String.format("ws://localhost:%d", port);
    }

    private Peer getPeer() {
        //@formatter:off
        return new ConfiguredEndpointsBuilder()
                .setName("test")
                .setModules(Set.of(
                        new YangIdentifier("test-model-notification")
                        ))
                .setNotificationEndpoints(BindingMap.of(new NotificationEndpointsBuilder()
                        .setPath("{}")
                        .setEndpointUri(new Uri(getPath()))
                        .build()))
                .build();
        //@formatter:on
    }

    private static Absolute notificationPath(Module mod, String methodName) {
        return Absolute.of(QName.create(mod.getQNameModule().getNamespace(),
                mod.getQNameModule().getRevision(), methodName));
    }
}
