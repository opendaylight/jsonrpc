/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.jsonrpc.bus.messagelib.DefaultTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.MessageLibrary;
import org.opendaylight.jsonrpc.bus.messagelib.ResponderSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.ConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataConfigEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataConfigEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataConfigEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataOperationalEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataOperationalEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataOperationalEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.NotificationEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.NotificationEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpointsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Tests for {@link JsonRPCProvider}.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class JsonRPCProviderTest extends AbstractJsonRpcTest {
    private static final String DEMO1_MODEL = "demo1";
    private static final String TOASTER = "toaster";
    private static final InstanceIdentifier<Config> GLOBAL_CFG_II = InstanceIdentifier.create(Config.class);
    private JsonRPCProvider provider;
    private int omPort;
    private int governancePort;
    private int dummyPort;
    private MessageLibrary messaging;
    private ResponderSession omResponder;
    private ResponderSession governanceResponder;
    private TransportFactory tf;
    private ResponderSession dummyResponder;
    private ScheduledExecutorService exec;

    @Before
    public void setUp() throws TransactionCommitFailedException {
        exec = Executors.newScheduledThreadPool(1);
        NormalizedNodesHelper.init(schemaContext);
        tf = new DefaultTransportFactory();
        omPort = getFreeTcpPort();
        governancePort = getFreeTcpPort();
        dummyPort = getFreeTcpPort();
        startZeroMq();
        updateConfig(
                new ConfigBuilder().setGovernanceRoot(new Uri(String.format("zmq://localhost:%d", omPort))).build());
        provider = new JsonRPCProvider();
        provider.setTransportFactory(tf);
        provider.setDataBroker(getDataBroker());
        provider.setDomDataBroker(getDomBroker());
        provider.setSchemaService(getSchemaService());
        provider.setCodec(NormalizedNodesHelper.getBindingToNormalizedNodeCodec());
        provider.setDomMountPointService(getDOMMountPointService());
        provider.setScheduledExecutorService(exec);
        provider.init();
    }

    @After
    public void tearDown() throws Exception {
        stopZeroMq();
        provider.close();
        exec.shutdownNow();
    }

    @Test(timeout = 15_000)
    public void testMountUnmount() throws InterruptedException, ExecutionException, ReadFailedException {
        DataConfigEndpointsBuilder dataConfigEndpointsBuilder = new DataConfigEndpointsBuilder();
        dataConfigEndpointsBuilder.setEndpointUri(new Uri(String.format("zmq://localhost:%d", governancePort)));
        dataConfigEndpointsBuilder.setPath("{}");
        List<DataConfigEndpoints> configList = new ArrayList<>();
        configList.add(dataConfigEndpointsBuilder.build());

        DataOperationalEndpointsBuilder dataOperEndpointsBuilder = new DataOperationalEndpointsBuilder();
        dataOperEndpointsBuilder.setEndpointUri(new Uri(String.format("zmq://localhost:%d", governancePort)));
        dataOperEndpointsBuilder.setPath("{}");
        List<DataOperationalEndpoints> operList = new ArrayList<>();
        operList.add(dataOperEndpointsBuilder.build());
        final ConfiguredEndpoints ep = new ConfiguredEndpointsBuilder().setDataConfigEndpoints(configList)
                .setDataOperationalEndpoints(operList).setModules(Lists.newArrayList(new YangIdentifier(DEMO1_MODEL)))
                .setName(TOASTER).build();
        retryAction(TimeUnit.SECONDS, 5, () -> provider.doMountDevice(ep));

        final YangInstanceIdentifier yii = Util.createBiPath(TOASTER);
        retryAction(TimeUnit.SECONDS, 5, getDOMMountPointService().getMountPoint(yii)::isPresent);

        // Verify that peer appeared
        retryAction(TimeUnit.SECONDS, 2, () -> TOASTER.equals(getPeerOpState(TOASTER).get().getName()));
        provider.forceRefresh(null).get();
        // Verify that peer vanished
        retryAction(TimeUnit.SECONDS, 2, () -> !getPeerOpState(TOASTER).isPresent());
        Optional<DOMMountPoint> mp = this.getDOMMountPointService().getMountPoint(yii);
        assertTrue(!mp.isPresent());
    }

    // Dummy test to check logic and gain coverage
    @Test
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
        //@formatter:off
        updateConfig(new ConfigBuilder()
                .setGovernanceRoot(new Uri(String.format("zmq://localhost:%d", omPort)))
                .setWhoAmI(new Uri(String.format("zmq://localhost:%d", getFreeTcpPort())))
                .setConfiguredEndpoints(Lists.newArrayList(
                        new ConfiguredEndpointsBuilder().setName(DEMO1_MODEL)
                            .setModules(Lists.newArrayList(new YangIdentifier(DEMO1_MODEL)))
                            .setDataConfigEndpoints(Lists.newArrayList(
                                    new DataConfigEndpointsBuilder()
                                        .setKey(new DataConfigEndpointsKey("{}"))
                                        .setEndpointUri(new Uri(dummyUri())).build()))
                                    .build()))
                        .build());
        //@formatter:on
        retryAction(TimeUnit.SECONDS, 3, () -> getPeerOpState(DEMO1_MODEL).isPresent());
    }

    // Test build-in (global SchemaContext)
    @Test(timeout = 15_000)
    public void test_BuiltinSchemaContextProvider() throws Exception {
        // unconfigure all
        updateConfig(new ConfigBuilder().build());
        // wait until nothing there
        retryAction(TimeUnit.SECONDS, 2, () -> !getPeerOpState(DEMO1_MODEL).isPresent());
        //@formatter:off
        updateConfig(new ConfigBuilder()
                .setWhoAmI(new Uri(String.format("zmq://localhost:%d", getFreeTcpPort())))
                .setConfiguredEndpoints(Lists.newArrayList(
                        new ConfiguredEndpointsBuilder().setName("test-model")
                            .setModules(Lists.newArrayList(new YangIdentifier("test-model")))
                            .setRpcEndpoints(Lists.newArrayList(
                                    new RpcEndpointsBuilder()
                                        .setKey(new RpcEndpointsKey("{}"))
                                        .setEndpointUri(new Uri(dummyUri()))
                                    .build()))
                            .setNotificationEndpoints(Lists.newArrayList(
                                    new NotificationEndpointsBuilder()
                                        .setKey(new NotificationEndpointsKey("{}"))
                                        .setEndpointUri(new Uri(dummyUri()))
                                    .build()))
                            .setDataOperationalEndpoints(Lists.newArrayList(
                                    new DataOperationalEndpointsBuilder()
                                        .setKey(new DataOperationalEndpointsKey("{}"))
                                        .setEndpointUri(new Uri(dummyUri()))
                                     .build()))
                            .setDataConfigEndpoints(Lists.newArrayList(
                                    new DataConfigEndpointsBuilder()
                                        .setKey(new DataConfigEndpointsKey("{}"))
                                        .setEndpointUri(new Uri(dummyUri())).build()))
                                    .build()))
                        .build());
        //@formatter:on
        retryAction(TimeUnit.SECONDS, 3, () -> getPeerOpState("test-model").isPresent());
    }

    /**
     * Test that peer with only operation state can be mounted.
     * See bug report https://jira.opendaylight.org/browse/JSONRPC-14
     */
    @Test
    public void test_OpStateOnlyModel_NoGovernance() throws Exception {
        // unconfigure all
        updateConfig(new ConfigBuilder().build());
        // wait until nothing there
        retryAction(TimeUnit.SECONDS, 2, () -> !getPeerOpState("test-model-op-only").isPresent());
        //@formatter:off
        updateConfig(new ConfigBuilder()
                .setWhoAmI(new Uri(String.format("zmq://localhost:%d", getFreeTcpPort())))
                .setConfiguredEndpoints(Lists.newArrayList(
                        new ConfiguredEndpointsBuilder().setName("test-model-op-only")
                            .setModules(Lists.newArrayList(new YangIdentifier("test-model-op-only")))
                            .setDataOperationalEndpoints(Lists.newArrayList(
                                    new DataOperationalEndpointsBuilder()
                                        .setKey(new DataOperationalEndpointsKey("{}"))
                                        .setEndpointUri(new Uri(dummyUri()))
                                     .build()))
                            .build()))
                        .build());
        //@formatter:on
        retryAction(TimeUnit.SECONDS, 3, () -> getPeerOpState("test-model-op-only").isPresent());
    }

    @SuppressWarnings("deprecation")
    private Optional<ActualEndpoints> getPeerOpState(String name) throws ReadFailedException {
        final ReadOnlyTransaction rtx = getDataBroker().newReadOnlyTransaction();
        try {
            return rtx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(Config.class)
                    .child(ActualEndpoints.class, new ActualEndpointsKey(name)).build()).checkedGet();
        } finally {
            rtx.close();
        }
    }

    @SuppressWarnings("deprecation")
    private void updateConfig(Config config) throws TransactionCommitFailedException {
        final WriteTransaction wrTrx = getDataBroker().newWriteOnlyTransaction();
        wrTrx.put(LogicalDatastoreType.CONFIGURATION, GLOBAL_CFG_II, config);
        wrTrx.submit().checkedGet();
    }

    private void startZeroMq() {
        messaging = new MessageLibrary("zmq");
        governanceResponder = messaging.responder(String.format("zmq://0.0.0.0:%d", governancePort),
                new GovernanceMessageHandler());
        omResponder = messaging.responder(String.format("zmq://0.0.0.0:%d", omPort),
                new OmRootMessageHandler(governancePort));
        dummyResponder = messaging.responder(String.format("zmq://0.0.0.0:%d", dummyPort), (request, replyBuilder) -> {
            // NOOP
        });
    }

    private void stopZeroMq() {
        omResponder.close();
        governanceResponder.close();
        dummyResponder.close();
        messaging.close();
    }

    private String dummyUri() {
        return String.format("zmq://localhost:%d", dummyPort);
    }
}
