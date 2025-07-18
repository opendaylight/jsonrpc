/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.messagelib.AbstractTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.MockTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.ReplyMessageHandler;
import org.opendaylight.jsonrpc.bus.messagelib.RequesterSession;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.jsonrpc.impl.JsonRpcDatastoreAdapter;
import org.opendaylight.jsonrpc.model.GovernanceProvider;
import org.opendaylight.jsonrpc.provider.cluster.impl.ClusterDependencies;
import org.opendaylight.jsonrpc.provider.cluster.impl.JsonRpcPeerListManager;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMMountPoint;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.eos.dom.simple.SimpleDOMEntityOwnershipService;
import org.opendaylight.mdsal.singleton.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.impl.EOSClusterSingletonServiceProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.MountStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataConfigEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataConfigEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.data.rev201014.TopContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.FactorialInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.binding.Rpc;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.binding.runtime.spi.BindingRuntimeHelpers;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
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
    private static final DataTreeIdentifier<ActualEndpoints> PEER_OP_DTI = DataTreeIdentifier
            .of(LogicalDatastoreType.OPERATIONAL, MOCK_PEER_OP_ID);
    private @Mock AbstractTransportFactory transportFactory;
    private EOSClusterSingletonServiceProvider clusterSingletonServiceProvider;
    private @Mock ClusterSingletonServiceProvider mockClusterSingletonServiceProvider;
    private @Mock ActorSystemProvider masterActorSystemProvider;
    private @Mock ActorSystemProvider slaveActorSystemProvider;
    private @Mock GovernanceProvider governanceProvider;
    private @Mock Registration mockSingletonRegistration;
    private @Mock RequesterSession rpcClient;
    private @Mock RpcProviderService rpcProviderService;
    private @Mock Registration rpcReg;
    private JsonRpcPeerListManager masterManager;
    private JsonRpcPeerListManager slaveManager;
    private TestCustomizer masterTestCustomizer;
    private TestCustomizer slaveTestCustomizer;
    private ActorSystem masterActorSystem;
    private ActorSystem slaveActorSystem;
    private JsonRpcCodecFactory masterConverter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        clusterSingletonServiceProvider = new EOSClusterSingletonServiceProvider(new SimpleDOMEntityOwnershipService());

        final YangXPathParserFactory yangXPathParserFactory = ServiceLoader
                .<YangXPathParserFactory>load(YangXPathParserFactory.class)
                .findFirst()
                .orElseThrow();

        doReturn(mockSingletonRegistration).when(mockClusterSingletonServiceProvider)
                .registerClusterSingletonService(any());
        doReturn(Optional.empty()).when(governanceProvider).get();
        masterActorSystem = ActorSystem.create("test", ConfigFactory.load().getConfig("Master"));
        doReturn(masterActorSystem).when(masterActorSystemProvider).getActorSystem();

        slaveActorSystem = ActorSystem.create("test", ConfigFactory.load().getConfig("Slave"));
        doReturn(slaveActorSystem).when(slaveActorSystemProvider).getActorSystem();

        masterTestCustomizer = newDataBrokerTest();
        slaveTestCustomizer = newDataBrokerTest();

        final MockTransportFactory tf = new MockTransportFactory(transportFactory);
        doReturn(JsonRpcReplyMessage.builder().build()).when(rpcClient)
                .sendRequestAndReadReply(anyString(), any(), any(JsonObject.class));
        doReturn(rpcClient).when(transportFactory)
                .createRequester(anyString(), any(ReplyMessageHandler.class), anyBoolean());
        doReturn(rpcReg).when(rpcProviderService).registerRpcImplementations(any(Rpc[].class));

        final ClusterDependencies masterDeps = new ClusterDependencies(tf, masterTestCustomizer.getDataBroker(),
                masterTestCustomizer.getDOMMountPointService(), masterTestCustomizer.getDomBroker(),
                masterTestCustomizer.getSchemaService(), masterTestCustomizer.getDOMNotificationRouter(),
                masterTestCustomizer.getDOMRpcRouter().rpcService(), yangXPathParserFactory, masterActorSystem,
                clusterSingletonServiceProvider, governanceProvider, rpcProviderService, null);

        final ClusterDependencies slaveDeps = new ClusterDependencies(tf, slaveTestCustomizer.getDataBroker(),
                slaveTestCustomizer.getDOMMountPointService(), slaveTestCustomizer.getDomBroker(),
                slaveTestCustomizer.getSchemaService(), slaveTestCustomizer.getDOMNotificationRouter(),
                slaveTestCustomizer.getDOMRpcRouter().rpcService(), yangXPathParserFactory, slaveActorSystem,
                mockClusterSingletonServiceProvider, governanceProvider, rpcProviderService, null);

        masterConverter = new JsonRpcCodecFactory(masterTestCustomizer.getSchemaService().getGlobalContext());
        JsonRpcDatastoreAdapter datastoreAdapter = new JsonRpcDatastoreAdapter(masterConverter,
                masterTestCustomizer.getDomBroker(), masterTestCustomizer.getSchemaService().getGlobalContext(),
                transportFactory) {
            @Override
            public void close() {
                // intentionally NOOP
            }

        };
        doReturn(datastoreAdapter).when(transportFactory).createRequesterProxy(any(), anyString(), anyBoolean());

        masterManager = new JsonRpcPeerListManager(masterDeps);
        slaveManager = new JsonRpcPeerListManager(slaveDeps);
    }

    @After
    public void tearDown() {
        masterManager.close();
        slaveManager.close();
        TestKit.shutdownActorSystem(masterActorSystem, true);
        TestKit.shutdownActorSystem(slaveActorSystem, true);
    }

    private static TestCustomizer newDataBrokerTest() throws Exception {
        final TestCustomizer dataBrokerTest = new TestCustomizer() {
            @Override
            protected Set<YangModuleInfo> getModuleInfos() {
                return ImmutableSet.of(
                    BindingRuntimeHelpers.getYangModuleInfo(NetworkTopology.class),
                    BindingRuntimeHelpers.getYangModuleInfo(Topology.class),
                    BindingRuntimeHelpers.getYangModuleInfo(TopContainer.class),
                    BindingRuntimeHelpers.getYangModuleInfo(Config.class),
                    BindingRuntimeHelpers.getYangModuleInfo(FactorialInput.class));
            }
        };

        dataBrokerTest.setup();
        return dataBrokerTest;
    }

    private static void createPeer(DataBroker dataBroker) throws InterruptedException, ExecutionException {
        final WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.CONFIGURATION, MOCK_PEER_CFG_ID, new ConfiguredEndpointsBuilder()
                .setName("device-1")
                .setModules(Set.of(new YangIdentifier("network-topology"), new YangIdentifier("test-model-data"),
                        new YangIdentifier("test-model-rpc")))
                .setDataConfigEndpoints(ImmutableMap.of(new DataConfigEndpointsKey("{}"),
                        new DataConfigEndpointsBuilder().setPath("{}")
                                .setEndpointUri(new Uri("zmq://localhost:10000"))
                                .build()))
                .setRpcEndpoints(ImmutableMap.of(new RpcEndpointsKey("{}"),
                        new RpcEndpointsBuilder().setPath("{}")
                                .setEndpointUri(new Uri("zmq://localhost:10000"))
                                .build()))
                .build());
        wtx.commit().get();
    }

    private static Optional<ActualEndpoints> getOpState(DataBroker dataBroker) {
        try (ReadTransaction rtx = dataBroker.newReadOnlyTransaction()) {
            return Futures.getUnchecked(rtx.read(LogicalDatastoreType.OPERATIONAL, MOCK_PEER_OP_ID));
        }
    }

    private void testMaster() throws InterruptedException, ExecutionException, IOException {
        createPeer(masterTestCustomizer.getDataBroker());
        await().atMost(TEN_SECONDS)
                .until(() -> getMountPoint(masterTestCustomizer.getDOMMountPointService()).isPresent());

        await().atMost(TEN_SECONDS).until(() -> getOpState(masterTestCustomizer.getDataBroker()).isPresent());

        final ActualEndpoints opState = getOpState(masterTestCustomizer.getDataBroker()).orElseThrow();
        assertEquals(MountStatus.Mounted, opState.getMountStatus());
        assertNotNull(opState.getManagedBy());

        DOMRpcService rpcService = getMountPoint(masterTestCustomizer.getDOMMountPointService()).orElseThrow()
                .getService(DOMRpcService.class)
                .orElseThrow();

        RpcDefinition rpc = Util.findNode(masterTestCustomizer.getSchemaService().getGlobalContext(),
                "test-model-rpc:removeCoffeePot", Module::getRpcs)
                .orElseThrow();
        ListenableFuture<? extends DOMRpcResult> rpcResult = rpcService.invokeRpc(rpc.getQName(), null);

        assertNotNull(rpcResult.get());

        DOMDataBroker domDataBroker = getMountPoint(masterTestCustomizer.getDOMMountPointService()).orElseThrow()
                .getService(DOMDataBroker.class)
                .orElseThrow();

        NormalizedNode data = masterConverter
                .dataCodec(YangInstanceIdentifier.builder().node(NetworkTopology.QNAME).build())
                .deserialize(JsonParser.parseString("{\"topology\": [{\"topology-id\": \"topology-1\"}]}"));
        final DOMDataTreeReadWriteTransaction wtx = domDataBroker.newReadWriteTransaction();
        wtx.put(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.of(NetworkTopology.QNAME), data);
        wtx.commit().get();

        try (DOMDataTreeReadTransaction rtx = domDataBroker.newReadOnlyTransaction()) {
            assertTrue(Futures.getUnchecked(
                    rtx.read(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.of(NetworkTopology.QNAME)))
                    .isPresent());
        }
    }

    private void testSlave() throws InterruptedException, ExecutionException {
        masterTestCustomizer.getDataBroker().registerTreeChangeListener(PEER_OP_DTI, changes -> {
            final WriteTransaction slaveTx = slaveTestCustomizer.getDataBroker().newWriteOnlyTransaction();
            for (DataTreeModification<ActualEndpoints> dtm : changes) {
                LOG.info("Change to copy : {}", dtm);
                DataObjectModification<ActualEndpoints> rootNode = dtm.getRootNode();
                InstanceIdentifier<ActualEndpoints> path = dtm.getRootPath().path();
                switch (rootNode.modificationType()) {
                    case WRITE:
                    case SUBTREE_MODIFIED:
                        slaveTx.merge(LogicalDatastoreType.OPERATIONAL, path, rootNode.dataAfter());
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

        createPeer(slaveTestCustomizer.getDataBroker());

        verify(mockClusterSingletonServiceProvider, timeout(5000)).registerClusterSingletonService(any());

        await().atMost(TEN_SECONDS)
                .until(() -> getMountPoint(slaveTestCustomizer.getDOMMountPointService()).isPresent());

        DOMRpcService rpcService = getMountPoint(slaveTestCustomizer.getDOMMountPointService()).orElseThrow()
                .getService(DOMRpcService.class)
                .orElseThrow();

        RpcDefinition rpc = Util
                .findNode(slaveTestCustomizer.getSchemaService().getGlobalContext(), "test-model-rpc:removeCoffeePot",
                        Module::getRpcs)
                .orElseThrow();
        ListenableFuture<? extends DOMRpcResult> rpcResult = rpcService.invokeRpc(rpc.getQName(), null);
        assertNotNull(rpcResult.get());

    }

    private static Optional<DOMMountPoint> getMountPoint(DOMMountPointService mountService) {
        return mountService.getMountPoint(Util.createBiPath("device-1"));
    }

    @Test
    public void test() throws InterruptedException, ExecutionException, IOException {
        LOG.info("Test master mountpoint");
        testMaster();
        LOG.info("Test slave mountpoint");
        testSlave();
    }
}
