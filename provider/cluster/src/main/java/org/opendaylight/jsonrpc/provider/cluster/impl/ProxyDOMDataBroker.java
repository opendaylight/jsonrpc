/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.impl;

import static org.opendaylight.jsonrpc.provider.cluster.impl.ClusterUtil.DEFAULT_ASK_TIMEOUT;
import static org.opendaylight.jsonrpc.provider.cluster.impl.ClusterUtil.durationFromUint16seconds;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import java.util.concurrent.TimeUnit;
import org.opendaylight.jsonrpc.provider.cluster.tx.ProxyReadTransaction;
import org.opendaylight.jsonrpc.provider.cluster.tx.ProxyReadWriteTransaction;
import org.opendaylight.jsonrpc.provider.cluster.tx.TxRequest;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.spi.PingPongMergingDOMDataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * Implementation of {@link DOMDataBroker} that forward all requests to actor on master node.
 *
 * <p>Acknowledgement : this code is inspired by implementation of netconf-topology-singleton.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 11, 2020
 */
final class ProxyDOMDataBroker implements PingPongMergingDOMDataBroker {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyDOMDataBroker.class);
    private final Peer peer;
    private final Timeout askTimeout;
    private final ActorRef masterActorRef;
    private final ExecutionContext dispatcher;

    ProxyDOMDataBroker(Peer peer, ActorRef masterActorRef, ClusterDependencies dependencies) {
        this.peer = peer;
        final Duration askDuration = dependencies.getConfig() == null ? DEFAULT_ASK_TIMEOUT
                : durationFromUint16seconds(dependencies.getConfig().getActorResponseWaitTime(), DEFAULT_ASK_TIMEOUT);
        askTimeout = Timeout.apply(askDuration.toSeconds(), TimeUnit.SECONDS);
        this.masterActorRef = masterActorRef;
        dispatcher = dependencies.getActorSystem().dispatcher();
        LOG.debug("Created {}", this);
    }

    @Override
    public DOMDataTreeReadTransaction newReadOnlyTransaction() {
        LOG.debug("[{}] new ROT via {}", peer.getName(), masterActorRef);
        final Future<Object> txActorFuture = Patterns.ask(masterActorRef, new TxRequest(), askTimeout);
        return new ProxyReadTransaction(peer, txActorFuture, dispatcher, askTimeout);
    }

    @Override
    public DOMDataTreeReadWriteTransaction newReadWriteTransaction() {
        LOG.debug("[{}] new RWT via {}", peer.getName(), masterActorRef);
        final Future<Object> future = Patterns.ask(masterActorRef, new TxRequest(), askTimeout);
        return new ProxyReadWriteTransaction(peer, future, dispatcher, askTimeout);
    }

    @Override
    public DOMDataTreeWriteTransaction newWriteOnlyTransaction() {
        LOG.debug("[{}] new WOT via {}", peer.getName(), masterActorRef);
        final Future<Object> future = Patterns.ask(masterActorRef, new TxRequest(), askTimeout);
        return new ProxyReadWriteTransaction(peer, future, dispatcher, askTimeout);
    }

    // TODO : How about this?
    @Override
    public DOMTransactionChain createTransactionChain() {
        LOG.debug("[{}] new transaction chain", peer.getName());
        throw new UnsupportedOperationException("Transaction chains not supported for JSONRPC mount point");
    }

    @Override
    public String toString() {
        return "ProxyDOMDataBroker [peer=" + peer + ", askTimeout=" + askTimeout + ", masterActorRef=" + masterActorRef
                + "]";
    }
}
