/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import java.lang.reflect.InvocationHandler;

/**
 * This service provides clients with an ability to define a proxy interface to
 * a server. The proxy interface must be same as the one defined by the server.
 * To define an interface for a notification server (i.e. a publisher), all
 * methods must return void, otherwise a {@link ProxyServiceGenericException} is
 * thrown.
 *
 * @author Shaleen Saxena
 */
public interface ProxyService extends InvocationHandler {
    /**
     * Create {@link RequesterSession} proxy instance with specific timeout supplied (optionally) as query parameter
     * 'timeout'.
     *
     * @param uri address of remote responder instance to connect to
     * @param cls known API implemented by remote {@link ResponderSession}.
     * @param <T> type of API
     * @return proxied instance of T
     */
    <T extends AutoCloseable> T createRequesterProxy(String uri, Class<T> cls);

    /**
     * Create {@link PublisherSession} proxy instance with specific timeout supplied (optionally) as query parameter
     * 'timeout'.
     *
     * @param uri address of remote publisher instance to connect to
     * @param cls known API
     * @param <T> type of API
     * @return proxied instance of T
     */
    <T extends AutoCloseable> T createPublisherProxy(String uri, Class<T> cls);
}
