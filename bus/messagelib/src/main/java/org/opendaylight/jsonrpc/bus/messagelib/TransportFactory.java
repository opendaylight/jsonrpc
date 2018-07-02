/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import java.net.URI;
import java.net.URISyntaxException;

import org.opendaylight.jsonrpc.bus.api.Publisher;

/**
 * Abstraction layer to decouple transport factory implementations.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public interface TransportFactory extends AutoCloseable {
    int DEFAULT_TIMEOUT = 30000;

    /**
     * Create {@link PublisherSession} T proxy instance using given URI.
     *
     * @param clazz Type of class to proxy
     * @param rawUri URI pointing to responder
     * @param <T> proxy class
     * @return T proxy instance
     * @throws URISyntaxException when URI denoted by rawUri has invalid syntax
     */
    default <T extends AutoCloseable> T createPublisherProxy(Class<T> clazz, String rawUri) throws URISyntaxException {
        return createPublisherProxy(clazz, rawUri, DEFAULT_TIMEOUT);
    }

    /**
     * Create {@link PublisherSession} T proxy instance using given URI with
     * specific connection timeout.
     *
     * @param clazz Type of class to proxy
     * @param rawUri URI pointing to responder
     * @param <T> proxy class
     * @param timeout connection timeout in milliseconds
     * @return T proxy instance
     * @throws URISyntaxException when URI denoted by rawUri has invalid syntax
     */
    <T extends AutoCloseable> T createPublisherProxy(Class<T> clazz, String rawUri, long timeout)
            throws URISyntaxException;

    /**
     * Create {@link RequesterSession} T proxy instance using given URI.
     *
     * @param clazz Type of class to proxy
     * @param rawUri URI pointing to responder
     * @param <T> proxy class
     * @return T proxy instance
     * @throws URISyntaxException when URI denoted by rawUri has invalid syntax
     */
    default <T extends AutoCloseable> T createRequesterProxy(Class<T> clazz, String rawUri) throws URISyntaxException {
        return createRequesterProxy(clazz, rawUri, DEFAULT_TIMEOUT);
    }

    /**
     * Create {@link RequesterSession} T proxy instance using given URI with
     * specific connection timeout.
     *
     * @param clazz Type of class to proxy
     * @param rawUri URI pointing to responder
     * @param <T> proxy class
     * @param timeout connection timeout
     * @return T proxy instance
     * @throws URISyntaxException when URI denoted by rawUri has invalid syntax
     */
    <T extends AutoCloseable> T createRequesterProxy(Class<T> clazz, String rawUri, long timeout)
            throws URISyntaxException;

    /**
     * Create {@link ResponderSession} for given {@link URI}.
     *
     * @param rawUri URI where new service will be published
     * @param handler used to handle JSON-RPC requests
     * @param <T> an AutoCloseable implementation of ThreadedSession
     * @return ThreadedSession
     * @throws URISyntaxException when URI denoted by rawUri has invalid syntax
     */
    <T extends AutoCloseable> ResponderSession createResponder(String rawUri, T handler) throws URISyntaxException;

    /**
     * Create {@link SubscriberSession} to {@link Publisher} endpoint. Specified
     * handler is used to handle requests.
     *
     * @param rawUri URI pointing to remote service implementing responder
     * @param handler Handler used to handle requests
     * @param <T> an AutoCloseable implementation of ThreadedSession
     * @return ThreadedSession
     * @throws URISyntaxException when URI denoted by rawUri has invalid syntax
     */
    <T extends AutoCloseable> SubscriberSession createSubscriber(String rawUri, T handler)
            throws URISyntaxException;

    /**
     * Create general session, actual transport and socket type is determined
     * based on URI scheme and query parameter 'role', which is mandatory.
     *
     * @param rawUri raw uri specification, can be transport specific
     * @param handler handler to be invoked on message response
     * @return {@link RequesterSession}
     * @throws URISyntaxException when URI denoted by rawUri has invalid syntax
     * @throws IllegalArgumentException when URI is missing role parameter
     */
    RequesterSession createRequester(String rawUri, ReplyMessageHandler handler) throws URISyntaxException;
}
