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

import java.time.Duration;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.pattern.Patterns;
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
import scala.jdk.javaapi.DurationConverters;

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
    private final Duration askTimeout;
    private final ActorRef masterActorRef;

    ProxyDOMDataBroker(final Peer peer, final ActorRef masterActorRef, final ClusterDependencies dependencies) {
        this.peer = peer;
        final var askDuration = dependencies.getConfig() == null ? DEFAULT_ASK_TIMEOUT
                : durationFromUint16seconds(dependencies.getConfig().getActorResponseWaitTime(), DEFAULT_ASK_TIMEOUT);
        askTimeout = DurationConverters.toJava(askDuration);
        this.masterActorRef = masterActorRef;
        LOG.debug("Created {}", this);
    }

    @Override
    public DOMDataTreeReadTransaction newReadOnlyTransaction() {
        LOG.debug("[{}] new ROT via {}", peer.getName(), masterActorRef);
        return new ProxyReadTransaction(peer, Patterns.ask(masterActorRef, new TxRequest(), askTimeout), askTimeout);
    }

    @Override
    public DOMDataTreeReadWriteTransaction newReadWriteTransaction() {
        LOG.debug("[{}] new RWT via {}", peer.getName(), masterActorRef);
        return new ProxyReadWriteTransaction(peer, Patterns.ask(masterActorRef, new TxRequest(), askTimeout),
            askTimeout);
    }

    @Override
    public DOMDataTreeWriteTransaction newWriteOnlyTransaction() {
        LOG.debug("[{}] new WOT via {}", peer.getName(), masterActorRef);
        return new ProxyReadWriteTransaction(peer, Patterns.ask(masterActorRef, new TxRequest(), askTimeout),
            askTimeout);
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
