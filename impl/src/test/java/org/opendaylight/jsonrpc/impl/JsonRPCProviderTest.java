/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;

import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.messagelib.MockTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.RequesterSession;
import org.opendaylight.jsonrpc.bus.messagelib.SubscriberSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.model.InbandModelsService;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataConfigEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataConfigEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataOperationalEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataOperationalEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.NotificationEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.NotificationEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpointsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Tests for {@link JsonRPCProvider}.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class JsonRPCProviderTest extends AbstractJsonRpcTest {
    private static final String DEMO1_MODEL = "demo1";
    private static final InstanceIdentifier<Config> GLOBAL_CFG_II = InstanceIdentifier.create(Config.class);
    private JsonRPCProvider provider;
    private static final RemoteGovernance GOVERNANCE_MOCK = new MockGovernance();
    private int governancePort;
    private int dummyPort;
    private TransportFactory tf;

    @Before
    public void setUp() throws URISyntaxException, InterruptedException, ExecutionException {
        tf = mock(TransportFactory.class);

        when(tf.createRequesterProxy(eq(RemoteGovernance.class), anyString(), anyBoolean()))
                .thenReturn(GOVERNANCE_MOCK);
        when(tf.createRequesterProxy(eq(InbandModelsService.class), anyString(), anyBoolean()))
                .thenReturn(new AbstractInbandModelsService() {
                });
        when(tf.createRequester(anyString(), any(), anyBoolean())).thenReturn(mock(RequesterSession.class));
        when(tf.createSubscriber(anyString(), any(), anyBoolean())).thenReturn(mock(SubscriberSession.class));
        governancePort = getFreeTcpPort();
        dummyPort = getFreeTcpPort();
        updateConfig(new ConfigBuilder().setGovernanceRoot(new Uri(String.format("zmq://localhost:%d", governancePort)))
                .build());
        provider = new JsonRPCProvider();
        provider.setTransportFactory(new MockTransportFactory(tf));
        provider.setDataBroker(getDataBroker());
        provider.setDomDataBroker(getDomBroker());
        provider.setSchemaService(getSchemaService());
        provider.setDomMountPointService(getDOMMountPointService());
        provider.setDomNotificationPublishService(getDOMNotificationRouter());
        provider.setDomRpcService(getDOMRpcRouter().getRpcService());
        provider.init();
        logTestName("START");
    }

    @After
    public void tearDown() throws Exception {
        logTestName("END");
        provider.close();
        reset(tf);
    }

    // Dummy test to check logic and gain coverage
    @Test(timeout = 15_000)
    public void test_UnmountEmpty() {
        assertFalse(provider.doUnmount(""));
        assertFalse(provider.doUnmount(null));
    }

    // Here we mount 'device' by updating configuration
    @Test(timeout = 15_000)
    public void test_ConfigDriven() throws Exception {
        // unconfigure all
        updateConfig(new ConfigBuilder().build());
        // wait until nothing there
        retryAction(TimeUnit.SECONDS, 2, () -> !getPeerOpState(DEMO1_MODEL).isPresent());
        updateConfig(new ConfigBuilder()
                .setGovernanceRoot(new Uri(String.format("zmq://localhost:%d", governancePort)))
                .setWhoAmI(new Uri(String.format("zmq://localhost:%d", getFreeTcpPort())))
                .setConfiguredEndpoints(Lists.newArrayList(
                        new ConfiguredEndpointsBuilder().setName(DEMO1_MODEL)
                            .setModules(Lists.newArrayList(new YangIdentifier(DEMO1_MODEL)))
                            .setDataConfigEndpoints(Lists.newArrayList(
                                    new DataConfigEndpointsBuilder()
                                        .withKey(new DataConfigEndpointsKey("{}"))
                                        .setEndpointUri(new Uri(dummyUri())).build()))
                                    .build()))
                        .build());
        //@formatter:on
        retryAction(TimeUnit.SECONDS, 3, () -> getPeerOpState(DEMO1_MODEL).isPresent());

        provider.forceReload(null).get();

        retryAction(TimeUnit.SECONDS, 3, () -> getPeerOpState(DEMO1_MODEL).isPresent());
    }

    // Test build-in (global SchemaContext)
    @Test(timeout = 15_000)
    public void test_BuiltinSchemaContextProvider() throws Exception {
        // unconfigure all
        updateConfig(new ConfigBuilder().build());
        // wait until nothing there
        retryAction(TimeUnit.SECONDS, 2, () -> !getPeerOpState(DEMO1_MODEL).isPresent());
        updateConfig(new ConfigBuilder()
                .setWhoAmI(new Uri(String.format("zmq://localhost:%d", getFreeTcpPort())))
                .setConfiguredEndpoints(Lists.newArrayList(new ConfiguredEndpointsBuilder().setName("test-model")
                        .setModules(Lists.newArrayList(new YangIdentifier("test-model")))
                        .setRpcEndpoints(Lists.newArrayList(new RpcEndpointsBuilder().withKey(new RpcEndpointsKey("{}"))
                                .setEndpointUri(new Uri(dummyUri()))
                                .build()))
                        .setNotificationEndpoints(Lists.newArrayList(
                                new NotificationEndpointsBuilder().withKey(new NotificationEndpointsKey("{}"))
                                        .setEndpointUri(new Uri(dummyUri()))
                                        .build()))
                        .setDataOperationalEndpoints(Lists.newArrayList(
                                new DataOperationalEndpointsBuilder().withKey(new DataOperationalEndpointsKey("{}"))
                                        .setEndpointUri(new Uri(dummyUri()))
                                        .build()))
                        .setDataConfigEndpoints(Lists.newArrayList(new DataConfigEndpointsBuilder()
                                .withKey(new DataConfigEndpointsKey("{}")).setEndpointUri(new Uri(dummyUri())).build()))
                        .build()))
                .build());
        retryAction(TimeUnit.SECONDS, 3, () -> getPeerOpState("test-model").isPresent());
    }

    @Test
    public void test_InbandModelsSchemaContextProvider() throws Exception {
        // unconfigure all
        updateConfig(new ConfigBuilder().build());
        // wait until nothing there
        retryAction(TimeUnit.SECONDS, 2, () -> !getPeerOpState(DEMO1_MODEL).isPresent());
        //@formatter:off
        updateConfig(new ConfigBuilder()
                .setWhoAmI(new Uri(String.format("zmq://localhost:%d", getFreeTcpPort())))
                .setConfiguredEndpoints(Lists.newArrayList(
                        new ConfiguredEndpointsBuilder().setName(DEMO1_MODEL)
                            .setModules(Lists.newArrayList(new YangIdentifier("jsonrpc-inband-models")))
                            .setRpcEndpoints(Lists.newArrayList(
                                    new RpcEndpointsBuilder()
                                        .withKey(new RpcEndpointsKey("{}"))
                                        .setEndpointUri(new Uri(dummyUri()))
                                    .build()))
                            .setNotificationEndpoints(Lists.newArrayList(
                                    new NotificationEndpointsBuilder()
                                        .withKey(new NotificationEndpointsKey("{}"))
                                        .setEndpointUri(new Uri(dummyUri()))
                                    .build()))
                            .setDataOperationalEndpoints(Lists.newArrayList(
                                    new DataOperationalEndpointsBuilder()
                                        .withKey(new DataOperationalEndpointsKey("{}"))
                                        .setEndpointUri(new Uri(dummyUri()))
                                     .build()))
                            .setDataConfigEndpoints(Lists.newArrayList(
                                    new DataConfigEndpointsBuilder()
                                        .withKey(new DataConfigEndpointsKey("{}"))
                                        .setEndpointUri(new Uri(dummyUri())).build()))
                                    .build()))
                        .build());
        //@formatter:on
        retryAction(TimeUnit.SECONDS, 5, () -> getPeerOpState(DEMO1_MODEL).isPresent());
    }

    /**
     * Test that peer with only operation state can be mounted. See bug report
     * https://jira.opendaylight.org/browse/JSONRPC-14
     */
    @Test(timeout = 15_000)
    public void test_OpStateOnlyModel_NoGovernance() throws Exception {
        // unconfigure all
        updateConfig(new ConfigBuilder().build());
        // wait until nothing there
        retryAction(TimeUnit.SECONDS, 2, () -> !getPeerOpState("test-model-op-only").isPresent());
        updateConfig(new ConfigBuilder()
                .setWhoAmI(new Uri(String.format("zmq://localhost:%d", getFreeTcpPort())))
                .setConfiguredEndpoints(
                        Lists.newArrayList(new ConfiguredEndpointsBuilder().setName("test-model-op-only")
                                .setModules(Lists.newArrayList(new YangIdentifier("test-model-op-only")))
                                .setDataOperationalEndpoints(Lists.newArrayList(new DataOperationalEndpointsBuilder()
                                        .withKey(new DataOperationalEndpointsKey("{}"))
                                        .setEndpointUri(new Uri(dummyUri()))
                                        .build()))
                                .build()))
                .build());
        retryAction(TimeUnit.SECONDS, 3, () -> getPeerOpState("test-model-op-only").isPresent());
    }

    private Optional<ActualEndpoints> getPeerOpState(String name) throws InterruptedException, ExecutionException {
        final ReadTransaction rtx = getDataBroker().newReadOnlyTransaction();
        try {
            return rtx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(Config.class)
                    .child(ActualEndpoints.class, new ActualEndpointsKey(name))
                    .build()).get();
        } finally {
            rtx.close();
        }
    }

    private void updateConfig(Config config) throws InterruptedException, ExecutionException {
        final WriteTransaction wrTrx = getDataBroker().newWriteOnlyTransaction();
        wrTrx.put(LogicalDatastoreType.CONFIGURATION, GLOBAL_CFG_II, config);
        wrTrx.commit().get();
    }

    private String dummyUri() {
        return String.format("zmq://localhost:%d", dummyPort);
    }
}
