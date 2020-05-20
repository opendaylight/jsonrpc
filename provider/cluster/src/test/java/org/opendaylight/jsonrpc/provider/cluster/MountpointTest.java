/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.TEN_SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import akka.actor.ActorSystem;
import akka.util.Timeout;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.jsonrpc.bus.messagelib.MockTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.model.GovernanceProvider;
import org.opendaylight.jsonrpc.provider.cluster.impl.ClusterDependencies;
import org.opendaylight.jsonrpc.provider.cluster.impl.JsonRpcPeerListManager;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMMountPoint;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.eos.dom.simple.SimpleDOMEntityOwnershipService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.dom.impl.DOMClusterSingletonServiceProviderImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.MountStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.numbers.list.Numbers;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.xpath.api.YangXPathParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MountpointTest {
    private static final Logger LOG = LoggerFactory.getLogger(MountpointTest.class);
    private static final InstanceIdentifier<ActualEndpoints> MOCK_PEER_OP_ID = InstanceIdentifier.builder(Config.class)
            .child(ActualEndpoints.class, new ActualEndpointsKey("device-1"))
            .build();
    private static final InstanceIdentifier<ConfiguredEndpoints> MOCK_PEER_CFG_ID = InstanceIdentifier
            .builder(Config.class)
            .child(ConfiguredEndpoints.class, new ConfiguredEndpointsKey("device-1"))
            .build();
    private @Mock TransportFactory transportFactory;
    private DOMClusterSingletonServiceProviderImpl clusterSingletonServiceProvider;
    private @Mock ClusterSingletonServiceProvider mockClusterSingletonServiceProvider;
    private @Mock ActorSystemProvider masterActorSystemProvider;
    private @Mock ActorSystemProvider slaveActorSystemProvider;
    private @Mock GovernanceProvider governanceProvider;
    private @Mock ClusterSingletonServiceRegistration mockSingletonRegistration;
    private Timeout askTimeout = Timeout.create(Duration.ofSeconds(15));
    private JsonRpcPeerListManager masterManager;
    private JsonRpcPeerListManager slaveManager;
    private TestCustomizer masterTestCustomizer;
    private TestCustomizer slaveTestCustomizer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        clusterSingletonServiceProvider = new DOMClusterSingletonServiceProviderImpl(
                new SimpleDOMEntityOwnershipService());
        clusterSingletonServiceProvider.initializeProvider();

        final YangXPathParserFactory yangXPathParserFactory = ServiceLoader
                .<YangXPathParserFactory>load(YangXPathParserFactory.class)
                .findFirst()
                .orElseThrow();

        doReturn(mockSingletonRegistration).when(mockClusterSingletonServiceProvider)
                .registerClusterSingletonService(any());
        doReturn(Optional.empty()).when(governanceProvider).get();
        ActorSystem masterActorSystem = ActorSystem.create("test", ConfigFactory.load().getConfig("Master"));
        doReturn(masterActorSystem).when(masterActorSystemProvider).getActorSystem();

        ActorSystem slaveActorSystem = ActorSystem.create("test", ConfigFactory.load().getConfig("Slave"));
        doReturn(slaveActorSystem).when(slaveActorSystemProvider).getActorSystem();

        masterTestCustomizer = newDataBrokerTest();
        slaveTestCustomizer = newDataBrokerTest();

        masterTestCustomizer.getDataBroker()
                .registerDataTreeChangeListener(
                        DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, MOCK_PEER_OP_ID), changes -> {
                            final WriteTransaction slaveTx = slaveTestCustomizer.getDataBroker()
                                    .newWriteOnlyTransaction();
                            for (DataTreeModification<ActualEndpoints> dtm : changes) {
                                LOG.info("Change to copy : {}", dtm);
                                DataObjectModification<ActualEndpoints> rootNode = dtm.getRootNode();
                                InstanceIdentifier<ActualEndpoints> path = dtm.getRootPath().getRootIdentifier();
                                switch (rootNode.getModificationType()) {
                                    case WRITE:
                                    case SUBTREE_MODIFIED:
                                        slaveTx.merge(LogicalDatastoreType.OPERATIONAL, path, rootNode.getDataAfter());
                                        break;
                                    case DELETE:
                                        slaveTx.delete(LogicalDatastoreType.OPERATIONAL, path);
                                        break;
                                    default:
                                        break;
                                }
                            }

                            slaveTx.commit();
                        });

        final MockTransportFactory tf = new MockTransportFactory(transportFactory);
        final ClusterDependencies masterDeps = new ClusterDependencies(tf, masterTestCustomizer.getDataBroker(),
                masterTestCustomizer.getDOMMountPointService(), masterTestCustomizer.getDomBroker(),
                masterTestCustomizer.getSchemaService(), masterTestCustomizer.getDOMNotificationRouter(),
                masterTestCustomizer.getDOMRpcRouter().getRpcService(), yangXPathParserFactory, masterActorSystem,
                clusterSingletonServiceProvider, governanceProvider);

        final ClusterDependencies slaveDeps = new ClusterDependencies(tf, slaveTestCustomizer.getDataBroker(),
                slaveTestCustomizer.getDOMMountPointService(), slaveTestCustomizer.getDomBroker(),
                slaveTestCustomizer.getSchemaService(), slaveTestCustomizer.getDOMNotificationRouter(),
                slaveTestCustomizer.getDOMRpcRouter().getRpcService(), yangXPathParserFactory, slaveActorSystem,
                mockClusterSingletonServiceProvider, governanceProvider);

        masterManager = new JsonRpcPeerListManager(masterDeps, askTimeout);
        slaveManager = new JsonRpcPeerListManager(slaveDeps, askTimeout);
    }

    @After
    public void tearDown() {
        masterManager.close();
        slaveManager.close();
    }

    private TestCustomizer newDataBrokerTest() throws Exception {
        final TestCustomizer dataBrokerTest = new TestCustomizer() {
            @Override
            protected Set<YangModuleInfo> getModuleInfos() throws Exception {
                return ImmutableSet.of(BindingReflections.getModuleInfo(NetworkTopology.class),
                        BindingReflections.getModuleInfo(Topology.class),
                        BindingReflections.getModuleInfo(Numbers.class),
                        BindingReflections.getModuleInfo(Config.class));
            }
        };

        dataBrokerTest.setup();
        return dataBrokerTest;
    }

    private void createPeer(DataBroker dataBroker) throws InterruptedException, ExecutionException {
        final WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.CONFIGURATION, MOCK_PEER_CFG_ID, new ConfiguredEndpointsBuilder()
                .setName("device-1")
                .setModules(
                        Lists.newArrayList(new YangIdentifier("network-topology"), new YangIdentifier("test-model")))
                .setRpcEndpoints(ImmutableMap.of(new RpcEndpointsKey("{}"),
                        new RpcEndpointsBuilder().setPath("{}")
                                .setEndpointUri(new Uri("zmq://localhost:10000"))
                                .build()))
                .build());
        wtx.commit().get();
    }

    private Optional<ActualEndpoints> getOpState(DataBroker dataBroker) {
        try (ReadTransaction rtx = dataBroker.newReadOnlyTransaction()) {
            return Futures.getUnchecked(rtx.read(LogicalDatastoreType.OPERATIONAL, MOCK_PEER_OP_ID));
        }
    }

    private void deletePeer(DataBroker dataBroker) throws InterruptedException, ExecutionException {
        final WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.delete(LogicalDatastoreType.CONFIGURATION, MOCK_PEER_CFG_ID);
        wtx.commit().get();
    }

    private void testMaster() throws InterruptedException, ExecutionException {
        createPeer(masterTestCustomizer.getDataBroker());
        await().atMost(TEN_SECONDS)
                .until(() -> getMountPoint(masterTestCustomizer.getDOMMountPointService()).isPresent());

        await().atMost(TEN_SECONDS).until(() -> getOpState(masterTestCustomizer.getDataBroker()).isPresent());

        final Optional<ActualEndpoints> opState = getOpState(masterTestCustomizer.getDataBroker());
        assertTrue(opState.isPresent());
        assertEquals(MountStatus.Mounted, opState.get().getMountStatus());
        assertNotNull(opState.get().getManagedBy());

        deletePeer(masterTestCustomizer.getDataBroker());
        await().atMost(TEN_SECONDS)
                .until(() -> !getMountPoint(masterTestCustomizer.getDOMMountPointService()).isPresent());
    }

    private void testSlave() throws InterruptedException, ExecutionException {
        createPeer(slaveTestCustomizer.getDataBroker());

        await().atMost(TEN_SECONDS)
                .until(() -> getMountPoint(slaveTestCustomizer.getDOMMountPointService()).isPresent());
    }

    private Optional<DOMMountPoint> getMountPoint(DOMMountPointService mountService) {
        return mountService.getMountPoint(Util.createBiPath("device-1"));
    }

    @Test
    public void test() throws InterruptedException, ExecutionException {
        LOG.info("Test master mountpoint");
        testMaster();
        LOG.info("Test slave mountpoint");
        testSlave();
    }
}
