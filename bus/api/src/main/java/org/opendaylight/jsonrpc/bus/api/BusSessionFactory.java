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
 * This factory is used to create different kinds of sessions for a given bus.
 * This interface needs to be implemented by various bus implementations.
 *
 * <p>Use {@link #close()} to close the underlying bus.
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
public interface BusSessionFactory extends AutoCloseable {

    /**
     * Create new {@link Publisher} to specific topic.
     *
     * @param uri transport specific URI to publish to
     * @return {@link Publisher} bound to address specified in URI
     */
    Publisher publisher(String uri);

    /**
     * Create new {@link Subscriber} to specific topic.
     *
     * @param uri transport specific URI to subscribe to
     * @param topic topic to subscribe to
     * @param listener {@link MessageListener} to be invoked on message
     *            reception.
     * @return {@link Subscriber} connected to address specified in URI
     */
    Subscriber subscriber(String uri, String topic, MessageListener listener);

    /**
     * Create new {@link Subscriber} to any topic.
     *
     * @param uri transport specific URI to subscribe to
     * @param listener {@link MessageListener} to be invoked on message
     *            reception.
     * @return {@link Subscriber} connected to address specified in URI
     */
    default Subscriber subscriber(String uri, MessageListener listener) {
        return subscriber(uri, "", listener);
    }

    /**
     * Create new {@link Requester}.
     *
     * @param uri address of remote {@link Responder} endpoint to connect to.
     * @param listener callback to be invoked after response arrived
     * @return {@link Requester} instance
     */
    Requester requester(String uri, MessageListener listener);

    /**
     * Create new {@link Responder}.
     *
     * @param uri endpoint to expose responder to
     * @param listener {@link MessageListener} to be invoked when request comes
     *            in
     * @return {@link Responder} instance
     */
    Responder responder(String uri, MessageListener listener);

    /**
     * Close this factory, eventually releasing all resources.
     */
    @Override
    void close();

    /**
     * Returns name of underlying transport, eg "zmq".
     *
     * @return name of underlying transport
     */
    String name();
}
