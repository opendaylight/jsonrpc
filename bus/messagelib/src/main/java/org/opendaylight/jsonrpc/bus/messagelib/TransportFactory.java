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

/**
 * Abstraction layer to decouple transport factory implementations
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public interface TransportFactory {
    /**
     * Create T proxy instance using given URI. EndpointRole is determined by
     * 'role' query parameter, which is mandatory
     *
     * @param clazz Type of class to proxy
     * @param rawUri URI pointing to responder
     * @return T instance
     * @throws URISyntaxException when URI denoted by rawUri has invalid syntax
     */
    <T extends AutoCloseable> T createProxy(Class<T> clazz, String rawUri) throws URISyntaxException;

    /**
     * Create Responder ThreadedSession for given {@link URI}
     *
     * @param rawUri URI where new service will be published
     * @param handler used to handle JSON-RPC requests
     * @param <T> an AutoCloseable implementation of ThreadedSession
     * @return ThreadedSession
     * @throws URISyntaxException when URI denoted by rawUri has invalid syntax
     */
    <T extends AutoCloseable> ThreadedSession createResponder(String rawUri, T handler) throws URISyntaxException;

    /**
     * Create {@link ThreadedSessionImpl} to Responder. Specified handler is
     * used to handle requests.
     * 
     * @param rawUri URI pointing to remote service implementing responder
     * @param handler Handler used to handle requests
     * @param <T> an AutoCloseable implementation of ThreadedSession
     * @return ThreadedSession
     * @throws URISyntaxException when URI denoted by rawUri has invalid syntax
     */
    <T extends AutoCloseable> ThreadedSession createSubscriber(String rawUri, T handler) throws URISyntaxException;

    /**
     * Create general session, actual transport and socket type is determined
     * based on URI scheme and query parameter 'role', which is mandatory.
     * 
     * @param rawUri raw uri specification, can be transport specific
     * @return {@link Session}
     * @throws URISyntaxException when URI denoted by rawUri has invalid syntax
     * @throws IllegalArgumentException when URI is missing role parameter
     */
    Session createSession(String rawUri) throws URISyntaxException;
}
