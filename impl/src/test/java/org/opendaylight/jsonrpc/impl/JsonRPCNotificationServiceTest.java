/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.jsonrpc.bus.messagelib.DefaultTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.EndpointRole;
import org.opendaylight.jsonrpc.bus.messagelib.MessageLibrary;
import org.opendaylight.jsonrpc.bus.messagelib.Session;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.Util;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumHashMap;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.hmap.JsonPathCodec;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.NotificationEndpointsBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
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
    private Session pubSession;
    private Module mod;
    private TransportFactory transportFactory;
    private final HierarchicalEnumMap<JsonElement, DataType, String> pathMap = HierarchicalEnumHashMap
            .create(DataType.class, JsonPathCodec.create());

    @Before
    public void setUp() throws URISyntaxException, ParseException {
        port = getFreeTcpPort();
        governance = mock(RemoteGovernance.class);
        when(governance.governance(anyInt(), anyString(), any())).thenReturn(getPath());
        mod = schemaContext.findModule("test-model", Revision.of("2016-11-17")).get();

        transportFactory = new DefaultTransportFactory();
        svc = new JsonRPCNotificationService(getPeer(),
                new BuiltinSchemaContextProvider(schemaContext).createSchemaContext(getPeer()), pathMap,
                transportFactory, governance);
        URI uri = new URI(getPath());
        pubSession = Util.openSession(new MessageLibrary(uri.getScheme()), uri, EndpointRole.PUB.name());
    }

    @After
    public void tearDown() {
        pubSession.close();
        svc.close();
    }

    @Test
    public void test() throws URISyntaxException, Exception {
        final CountDownLatch cl = new CountDownLatch(1);
        svc.registerNotificationListener((DOMNotificationListener) notification -> {
            LOG.info("Received notification : {}", notification);
            cl.countDown();
        }, notificationPath(mod, "too-many-numbers"));
        TimeUnit.MILLISECONDS.sleep(500L);
        pubSession.sendRequest("too-many-numbers", new int[] {});
        assertTrue(cl.await(5, TimeUnit.SECONDS));
    }

    private String getPath() {
        return String.format("zmq://localhost:%d", port);
    }

    private Peer getPeer() {
        //@formatter:off
        return new ConfiguredEndpointsBuilder()
                .setModules(Lists.newArrayList(
                        new YangIdentifier("test-model")
                        ))
                .setNotificationEndpoints(Lists.newArrayList(new NotificationEndpointsBuilder()
                        .setPath("{}")
                        .setEndpointUri(new Uri(getPath()))
                        .build()))
                .build();
        //@formatter:on
    }

    private static SchemaPath notificationPath(Module mod, String methodName) {
        return SchemaPath.create(true,
                QName.create(mod.getQNameModule().getNamespace(), mod.getQNameModule().getRevision(), methodName));
    }
}
