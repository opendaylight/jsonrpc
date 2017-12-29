/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus;

/**
 * This factory is used to create different kinds of sessions for a given bus.
 * This interface needs to be implemented by various bus implementations.
 *
 * <p>
 * Use {@link #close()} to close the underlying bus.
 * <dl>
 * <dt>publisher</dt>
 * <dd>To send/publish messages only</dd>
 * <dt>subscriber</dt>
 * <dd>To listen to messages from the publisher.</dd>
 * <dt>responder</dt>
 * <dd>Server to receive requests and send replies.</dd>
 * <dt>requester</dt>
 * <dd>Client to send requests and receive replies.</dd>
 * </dl>
 *
 * @author Shaleen Saxena
 */
public interface BusSessionFactory<T extends BusSession> extends AutoCloseable {
    /**
     * Create {@link SessionType#PUBLISHER} session to given URI with default topic (empty string).
     *
     * @param uri URI pointing to remote service
     * @return {@link BusSession}
     */
    default T publisher(String uri) {
        return publisher(uri, "");
    }

    /**
     * Create {@link SessionType#PUBLISHER} session to given URI with specified topic.
     *
     * @param uri URI pointing to remote service
     * @return {@link BusSession}
     */
    T publisher(String uri, String topic);

    /**
     * Create {@link SessionType#SUBSCRIBER} session to given URI with default topic (empty string).
     *
     * @param uri URI pointing to remote service
     * @return {@link BusSession}
     */
    default T subscriber(String uri) {
        return subscriber(uri, "");
    }

    /**
     * Create {@link SessionType#PUBLISHER} session to given URI with specified topic.
     *
     * @param uri URI pointing to remote service
     * @return {@link BusSession}
     */
    T subscriber(String uri, String topic);

    /**
     * Create {@link SessionType#RESPONDER} session to given URI.
     *
     * @param uri URI pointing to remote service
     * @return {@link BusSession}
     */
    T responder(String uri);

    /**
     * Create new {@link SessionType#REQUESTER} session to given URI.
     *
     * @param uri URI pointing to remote service
     * @return {@link BusSession}
     */
    T requester(String uri);

    /**
     * Close {@link BusSessionFactory}, eventually releasing all resources.
     */
    @Override
    void close();

    /**
     * Returns name of underlying transport, eg "zmq".
     */
    String name();
}
