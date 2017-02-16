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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
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
import org.opendaylight.jsonrpc.bus.messagelib.ThreadedSession;
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

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

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
    private MessageLibrary messaging;
    private ThreadedSession omResponder;
    private ThreadedSession governanceResponder;

    @Before
    public void setUp() throws TransactionCommitFailedException {
        NormalizedNodesHelper.init(schemaContext);
        omPort = getFreeTcpPort();
        governancePort = getFreeTcpPort();
        startZeroMq(omPort);
        updateConfig(
                new ConfigBuilder().setGovernanceRoot(new Uri(String.format("zmq://localhost:%d", omPort))).build());
        provider = new JsonRPCProvider();
        provider.setTransportFactory(new DefaultTransportFactory());
        provider.setDataBroker(getDataBroker());
        provider.setDomDataBroker(getDomBroker());
        provider.setSchemaService(getSchemaService());
        provider.setCodec(NormalizedNodesHelper.getBindingToNormalizedNodeCodec());
        provider.setDomMountPointService(getDOMMountPointService());
        provider.init();
    }

    @After
    public void tearDown() throws Exception {
        stopZeroMq();
        provider.close();
    }

    @Test(timeout=15000)
    public void testMountUnmount() throws InterruptedException, ExecutionException, ReadFailedException {
        DataConfigEndpointsBuilder dataConfigEndpointsBuilder = new DataConfigEndpointsBuilder();
        dataConfigEndpointsBuilder.setEndpointUri(new Uri(String.format("zmq://localhost:%d", governancePort)));
        dataConfigEndpointsBuilder.setPath("{}");
        List<DataConfigEndpoints> configList = new ArrayList<DataConfigEndpoints>();
        configList.add(dataConfigEndpointsBuilder.build());

        DataOperationalEndpointsBuilder dataOperEndpointsBuilder = new DataOperationalEndpointsBuilder();
        dataOperEndpointsBuilder.setEndpointUri(new Uri(String.format("zmq://localhost:%d", governancePort)));
        dataOperEndpointsBuilder.setPath("{}");
        List<DataOperationalEndpoints> operList = new ArrayList<DataOperationalEndpoints>();
        operList.add(dataOperEndpointsBuilder.build());
        final ConfiguredEndpoints ep = new ConfiguredEndpointsBuilder().setDataConfigEndpoints(configList)
                .setDataOperationalEndpoints(operList).setModules(Lists.newArrayList(new YangIdentifier(DEMO1_MODEL)))
                .setName(TOASTER).build();
        retryAction(TimeUnit.SECONDS, 5, () -> provider.doMountDevice(ep));

        final YangInstanceIdentifier yii = Util.createBiPath(TOASTER);
        retryAction(TimeUnit.SECONDS, 5, getDOMMountPointService().getMountPoint(yii)::isPresent);
        Optional<DOMMountPoint> mp = getDOMMountPointService().getMountPoint(yii);
        // Verify that peer appeared
        retryAction(TimeUnit.SECONDS, 2, () -> TOASTER.equals(getPeerOpState(TOASTER).get().getName()));
        provider.forceRefresh().get();
        // Verify that peer vanished
        retryAction(TimeUnit.SECONDS, 2, () -> !getPeerOpState(TOASTER).isPresent());
        mp = this.getDOMMountPointService().getMountPoint(yii);
        assertTrue(!mp.isPresent());
    }

    // Dummy test to check logic and gain coverage
    @Test
    public void test_UnmountEmpty() {
        assertFalse(provider.doUnmount(""));
        assertFalse(provider.doUnmount(null));
    }

    // Here we mount 'device' by updating configuration
    @Test(timeout=15000)
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
                                        .setEndpointUri(new Uri("zmq://localhost:12345")).build()))
                                    .build()))
                        .build());
        //@formatter:on
        retryAction(TimeUnit.SECONDS, 3, () -> getPeerOpState(DEMO1_MODEL).isPresent());
    }

    // Test build-in (global SchemaContext)
    @Test(timeout=15000)
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
                                        .setEndpointUri(new Uri("zmq://localhost:12345"))
                                    .build()))
                            .setNotificationEndpoints(Lists.newArrayList(
                                    new NotificationEndpointsBuilder()
                                        .setKey(new NotificationEndpointsKey("{}"))
                                        .setEndpointUri(new Uri("zmq://localhost:12345"))
                                    .build()))
                            .setDataOperationalEndpoints(Lists.newArrayList(
                                    new DataOperationalEndpointsBuilder()
                                        .setKey(new DataOperationalEndpointsKey("{}"))
                                        .setEndpointUri(new Uri("zmq://localhost:12345"))
                                     .build()))
                            .setDataConfigEndpoints(Lists.newArrayList(
                                    new DataConfigEndpointsBuilder()
                                        .setKey(new DataConfigEndpointsKey("{}"))
                                        .setEndpointUri(new Uri("zmq://localhost:12345")).build()))
                                    .build()))
                        .build());
        //@formatter:on
        retryAction(TimeUnit.SECONDS, 3, () -> getPeerOpState("test-model").isPresent());
    }

    private Optional<ActualEndpoints> getPeerOpState(String name) throws ReadFailedException {
        final ReadOnlyTransaction rtx = getDataBroker().newReadOnlyTransaction();
        try {
            return rtx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(Config.class)
                    .child(ActualEndpoints.class, new ActualEndpointsKey(name)).build()).checkedGet();
        } finally {
            rtx.close();
        }
    }

    private void updateConfig(Config config) throws TransactionCommitFailedException {
        final WriteTransaction wrTrx = getDataBroker().newWriteOnlyTransaction();
        wrTrx.put(LogicalDatastoreType.CONFIGURATION, GLOBAL_CFG_II, config);
        wrTrx.submit().checkedGet();
    }

    private void startZeroMq(int port) {
        messaging = new MessageLibrary("zmq");

        governanceResponder = messaging.threadedResponder(String.format("tcp://*:%d", governancePort),
                new GovernanceMessageHandler());
        omResponder = messaging.threadedResponder(String.format("tcp://*:%d", port),
                new OmRootMessageHandler(governancePort));
    }

    private void stopZeroMq() {
        omResponder.stop();
        governanceResponder.stop();
    }
}
