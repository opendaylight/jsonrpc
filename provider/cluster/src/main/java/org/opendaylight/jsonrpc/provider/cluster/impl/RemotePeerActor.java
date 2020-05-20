/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.impl;

import akka.actor.Props;
import akka.util.Timeout;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.jsonrpc.provider.cluster.messages.InvokeRpcRequest;
import org.opendaylight.jsonrpc.provider.common.ProviderDependencies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotePeerActor extends AbstractUntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(RemotePeerActor.class);

    public static Props props(final Peer peer, final Timeout timeout, final ProviderDependencies dependencies) {
        return Props.create(RemotePeerActor.class, () -> new RemotePeerActor(peer, timeout, dependencies));
    }

    private final Peer peer;

    public RemotePeerActor(Peer peer, Timeout timeout, ProviderDependencies dependencies) {
        this.peer = peer;
    }

    @Override
    protected void handleReceive(Object message) {
        LOG.debug("[{}] received : {}", peer.getName(), message);
        if (message instanceof InvokeRpcRequest) {
            invokeSlaveRpc();
        }

    }

    private void unregisterSlaveMountPoint() {
        // TODO Auto-generated method stub

    }

    @Override
    public void postStop() throws Exception {
        try {
            super.postStop();
        } finally {
            unregisterSlaveMountPoint();
        }
    }

    private void invokeSlaveRpc() {
        // TODO Auto-generated method stub

    }
}
