/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.remove;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.util.Timeout;
import org.opendaylight.jsonrpc.provider.common.AbstractPeerContext;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPoint;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

public class SlavePeerContext extends AbstractPeerContext {
    private Timeout timeout;
    private ActorSystem actorSystem;
    private ActorSelection masterActorRef;
    private final ObjectRegistration<DOMMountPoint> registration;

    SlavePeerContext(Peer peer, DataBroker dataBroker, DOMMountPointService domMountPointService) {
        super(peer, dataBroker);

        final YangInstanceIdentifier path = Util.createBiPath(peer.getName());
        EffectiveModelContext ctx = null; // TODO
        registration = domMountPointService.createMountPoint(path)
                .addInitialSchemaContext(ctx)
                .addService(DOMRpcService.class,
                        new SlaveDOMRpcService(actorSystem, timeout, masterActorRef, peer.getName()))
                .register();

    }

    @Override
    public void close() {
        registration.close();
        // intentionally not calling super#close() as there is nothing to remove from op. state
    }
}
