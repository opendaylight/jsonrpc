/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import java.net.URISyntaxException;

/**
 * Default implementation of {@link TransportFactory} normally used in
 * standalone applications and tests.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public class DefaultTransportFactory implements TransportFactory {
    /**
     * @see Util#createProxy(Class, String)
     */
    @Override
    public <T extends AutoCloseable> T createProxy(Class<T> clazz, String rawUri) throws URISyntaxException {
        return Util.createProxy(clazz, rawUri);
    }

    /**
     * @see Util#createThreadedResponderSession(String, AutoCloseable)
     */
    @Override
    public <T extends AutoCloseable> ThreadedSession createResponder(String rawUri, T handler)
            throws URISyntaxException {
        return Util.createThreadedResponderSession(rawUri, handler);
    }

    /**
     * @see Util#createThreadedSubscriberSession(String, AutoCloseable)
     */
    @Override
    public <T extends AutoCloseable> ThreadedSession createSubscriber(String rawUri, T handler)
            throws URISyntaxException {
        return Util.createThreadedSubscriberSession(rawUri, handler);
    }

    /**
     * @see Util#openSession(String)
     * @see Util#openSession(String, String)
     */
    @Override
    public Session createSession(String rawUri) throws URISyntaxException {
        return Util.openSession(rawUri);
    }
}
