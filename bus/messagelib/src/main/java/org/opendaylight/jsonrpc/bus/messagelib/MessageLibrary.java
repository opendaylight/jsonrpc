/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactoryProvider;

/**
 * This is the main class to create sessions over a bus. This class will help
 * create a {@link BusSessionFactory}, then use it to create sessions of various
 * types.
 *
 * @author Shaleen Saxena
 *
 */
public class MessageLibrary implements AutoCloseable, CloseCallback {
    private final BusSessionFactory factory;
    private final Collection<AbstractSession> sessions = ConcurrentHashMap.newKeySet();

    /**
     * Default constructor which uses {@link TcclBusSessionFactoryProvider} to
     * discover installed transports.
     *
     * @param busType bus type to get
     */
    public MessageLibrary(String busType) {
        // scan classpath using TCCL classloader and find service
        // implementations
        this(TcclBusSessionFactoryProvider.getInstance(), busType);
    }

    /**
     * Constructor which allows usage of custom
     * {@link TcclBusSessionFactoryProvider} to discover installed transports.
     *
     * @param bsfp Bus Session Factory Provider
     * @param busType bus type to get
     */
    public MessageLibrary(BusSessionFactoryProvider bsfp, String busType) {
        BusSessionFactory desiredFactory = null;
        final Iterator<BusSessionFactory> it = bsfp.getBusSessionFactories();
        while (it.hasNext()) {
            final BusSessionFactory f = it.next();
            if (busType.equalsIgnoreCase(f.name())) {
                desiredFactory = f;
                break;
            }
        }

        if (desiredFactory == null) {
            throw new IllegalArgumentException(String.format("Bus Type not supported : %s", busType));
        }

        this.factory = desiredFactory;
    }

    @Override
    public void close() {
        sessions.forEach(AbstractSession::close);
        factory.close();
    }

    /**
     * Create new {@link SubscriberSession}.
     *
     * @param uri URI of remote endpoint where publisher listen
     * @param handler instance of {@link NotificationMessageHandler} which will
     *            be invoked for every published message,
     * @return {@link SubscriberSession}
     */
    public SubscriberSession subscriber(String uri, NotificationMessageHandler handler) {
        final SubscriberSessionImpl session = new SubscriberSessionImpl(this, factory, handler, "", uri);
        sessions.add(session);
        return session;
    }

    public PublisherSession publisher(String uri) {
        final PublisherSessionImpl session = new PublisherSessionImpl(this, factory, uri);
        sessions.add(session);
        return session;
    }

    public RequesterSession requester(String uri, ReplyMessageHandler handler) {
        final RequesterSessionImpl session = new RequesterSessionImpl(this, factory, uri, handler);
        sessions.add(session);
        return session;
    }

    public ResponderSession responder(String uri, RequestMessageHandler handler) {
        final ResponderSessionImpl session = new ResponderSessionImpl(this, factory, handler, uri);
        sessions.add(session);
        return session;
    }

    @Override
    public void onClose(AutoCloseable closeable) {
        sessions.remove(closeable);
    }
}
