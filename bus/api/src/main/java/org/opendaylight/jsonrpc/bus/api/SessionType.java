/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.api;

/**
 * Session type.
 *
 * @author Shaleen Saxena
 */
public enum SessionType {
    /**
     * Requester.
     *
     * <p>
     * The REQ socket type acts as the client for a set of anonymous services,
     * sending requests and receiving replies using a lock-step round-robin
     * algorithm. It is designed for simple request-reply models where
     * reliability against failing peers is not an issue.
     * </p>
     * <p>
     * See <a href="https://rfc.zeromq.org/spec:28/REQREP/">specification</a>
     * </p>
     */
    REQ,

    /**
     * Replier.
     *
     * <p>
     * The REP socket type acts as as service for a set of client peers,
     * receiving requests and sending replies back to the requesting peers. It
     * is designed for simple remote-procedure call models.
     * </p>
     * <p>
     * See <a href="https://rfc.zeromq.org/spec:28/REQREP/">specification</a>
     * </p>
     */
    REP,

    /**
     * Publisher.
     *
     * <p>
     * The PUB socket type provides basic one-way broadcasting to a set of
     * subscribers. Over TCP, it does filtering on outgoing messages but
     * nonetheless a message will be sent multiple times over the network to
     * reach multiple subscribers. PUB is used mainly for transient event
     * distribution where stability of the network (e.g. consistently low memory
     * usage) is more important than reliability of traffic.
     * </p>
     * <p>
     * See
     * <a href="https://rfc.zeromq.org/spec:29/PUBSUB/">specification</a>
     * </p>
     *
     */
    PUB,

    /**
     * Subscriber.
     *
     * <p>
     * The SUB socket type provides a basic one-way listener for a set of
     * publishers.
     * </p>
     *
     * <p>
     * See <a href="https://rfc.zeromq.org/spec:29/PUBSUB/">specification</a>
     * </p>
     *
     */
    SUB
}
