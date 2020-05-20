/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.impl;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Status.Failure;
import akka.actor.Status.Success;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.jsonrpc.model.CombinedSchemaContextProvider;
import org.opendaylight.jsonrpc.provider.cluster.messages.InitCompleted;
import org.opendaylight.jsonrpc.provider.cluster.messages.InitMasterMountPoint;
import org.opendaylight.jsonrpc.provider.cluster.messages.MountPointRequest;
import org.opendaylight.jsonrpc.provider.cluster.messages.MountPointResponse;
import org.opendaylight.jsonrpc.provider.cluster.messages.PathAndDataMsg;
import org.opendaylight.jsonrpc.provider.cluster.messages.UnregisterMountPoint;
import org.opendaylight.jsonrpc.provider.cluster.rpc.EmptyRpcResponse;
import org.opendaylight.jsonrpc.provider.cluster.rpc.InvokeRpcRequest;
import org.opendaylight.jsonrpc.provider.cluster.rpc.InvokeRpcResponse;
import org.opendaylight.jsonrpc.provider.cluster.tx.TransactionActor;
import org.opendaylight.jsonrpc.provider.cluster.tx.TxRequest;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMMountPoint;
import org.opendaylight.mdsal.dom.api.DOMMountPointService.DOMMountPointBuilder;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 * Actor that handles both slave and master-originated messages.
 *
 * <p>
 * Acknowledgement : this code is inspired by implementation of netconf-topology-singleton.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 13, 2020
 */
class RemotePeerActor extends AbstractUntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(RemotePeerActor.class);

    public static Props props(final Peer peer, final ClusterDependencies dependencies) {
        return Props.create(RemotePeerActor.class, () -> new RemotePeerActor(peer, dependencies));
    }

    private final Peer peer;
    private final ClusterDependencies dependencies;
    private ObjectRegistration<DOMMountPoint> mountPointReg;
    private DOMDataBroker domDataBroker;
    private DOMRpcService domRpcService;
    private Duration writeTxIdleTimeout = Duration.apply(30, TimeUnit.SECONDS); // TODO : get from config

    RemotePeerActor(Peer peer, ClusterDependencies dependencies) {
        this.peer = peer;
        this.dependencies = dependencies;
    }

    @Override
    protected void handleReceive(Object message) {
        LOG.debug("[{}] received : {} from {}", peer.getName(), message, sender());
        if (message instanceof InvokeRpcRequest) {
            invokeSlaveRpc((InvokeRpcRequest) message);
        } else if (message instanceof InitMasterMountPoint) {
            initMasterMountpoint((InitMasterMountPoint) message);
        } else if (message instanceof MountPointRequest) {
            final MountPointRequest request = (MountPointRequest) message;
            request.getSlaveActorRef().tell(new MountPointResponse(self()), sender());
        } else if (message instanceof MountPointResponse) {
            final MountPointResponse response = (MountPointResponse) message;
            registerMountpoint(response.getMasterActorRef());
            sender().tell(new Success(null), self());
        } else if (message instanceof UnregisterMountPoint) {
            unregisterMountPoint();
        } else if (message instanceof TxRequest) {
            handleTxRequest((TxRequest) message);
        }
    }

    private void initMasterMountpoint(InitMasterMountPoint message) {
        domDataBroker = message.getDomDataBroker();
        domRpcService = message.getDomRpcService();
        sender().tell(new InitCompleted(), self());
    }

    private void handleTxRequest(TxRequest message) {
        try {
            sender().tell(new Success(context().actorOf(newTxActor())), self());
        } catch (Exception e) {
            sender().tell(new Failure(e), self());
        }
    }

    @Override
    public void postStop() throws Exception {
        try {
            super.postStop();
        } finally {
            unregisterMountPoint();
        }
    }

    private void registerMountpoint(ActorRef masterActorRef) {
        final CombinedSchemaContextProvider schemaFactory = new CombinedSchemaContextProvider(
                dependencies.getGovernanceProvider(), dependencies);

        final DOMMountPointBuilder builder = dependencies.getDomMountPointService()
                .createMountPoint(Util.createBiPath(peer.getName()));
        final EffectiveModelContext schema = schemaFactory.createSchemaContext(peer);
        builder.addInitialSchemaContext(schema);
        builder.addService(DOMDataBroker.class, new ProxyDOMDataBroker(peer, masterActorRef, dependencies));
        builder.addService(DOMRpcService.class, new ProxyDOMRpcService(peer, masterActorRef, dependencies));
        mountPointReg = builder.register();
        LOG.info("[{}] Slave mountpoint created", peer.getName());
    }

    private void unregisterMountPoint() {
        LOG.info("[{}] Unregistering mountpoint {}", peer.getName(), mountPointReg);
        Util.closeAndLogOnError(mountPointReg);
    }

    private Props newTxActor() {
        final DOMDataTreeReadWriteTransaction tx = domDataBroker.newReadWriteTransaction();
        return TransactionActor.props(tx, writeTxIdleTimeout);
    }

    private void invokeSlaveRpc(InvokeRpcRequest request) {
        final SchemaPath schemaPath = request.getSchemaPath().getPath();
        final PathAndDataMsg data = request.getData();

        final ListenableFuture<? extends DOMRpcResult> rpcResult = domRpcService.invokeRpc(schemaPath,
                data != null ? data.getData() : null);

        Futures.addCallback(rpcResult, new FutureCallback<DOMRpcResult>() {
            @Override
            public void onSuccess(final DOMRpcResult domRpcResult) {
                LOG.debug("[{}] RPC result for {}, domRpcResult: {}", peer.getName(), schemaPath, domRpcResult);

                if (domRpcResult == null) {
                    sender().tell(new EmptyRpcResponse(), getSender());
                    return;
                }
                PathAndDataMsg nodeMessageReply = null;
                if (domRpcResult.getResult() != null) {
                    nodeMessageReply = new PathAndDataMsg(YangInstanceIdentifier.empty(), domRpcResult.getResult());
                }
                sender().tell(new InvokeRpcResponse(nodeMessageReply, domRpcResult.getErrors()), getSelf());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                sender().tell(new Failure(throwable), getSelf());
            }
        }, MoreExecutors.directExecutor());
    }
}
