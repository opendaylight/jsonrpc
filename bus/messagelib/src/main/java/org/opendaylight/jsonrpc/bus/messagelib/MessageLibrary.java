/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactoryProvider;
import org.opendaylight.jsonrpc.bus.api.SessionType;

/**
 * This is the main class to create sessions over a bus. This class uses
 * {@link BusSessionFactoryProvider} to create a {@link BusSessionFactory}, then
 * use it to create sessions of various types.
 *
 * @author Shaleen Saxena
 *
 */
public class MessageLibrary implements AutoCloseable, Consumer<AutoCloseable> {
    private final BusSessionFactory factory;
    private final Collection<AbstractSession> sessions = ConcurrentHashMap.newKeySet();
    // Reuse sessions of same type, endpoint and handler
    private final LoadingCache<SessionKey, AbstractSession> sessionCache = CacheBuilder.newBuilder()
            .build(new CacheLoader<SessionKey, AbstractSession>() {
                @Override
                public AbstractSession load(SessionKey key) throws Exception {
                    final AbstractSession session;
                    switch (key.type()) {
                        case REQ:
                            session = new RequesterSessionImpl(MessageLibrary.this, factory, key.uri(),
                                    (ReplyMessageHandler) key.handler());
                            break;

                        case REP:
                            session = new ResponderSessionImpl(MessageLibrary.this, factory,
                                    (RequestMessageHandler) key.handler(), key.uri());
                            break;

                        case PUB:
                            session = new PublisherSessionImpl(MessageLibrary.this, factory, key.uri());
                            break;

                        case SUB:
                            session = new SubscriberSessionImpl(MessageLibrary.this, factory,
                                    (NotificationMessageHandler) key.handler(), "", key.uri());
                            break;

                        default:
                            throw new IllegalArgumentException("Unsupported session : " + key.type());
                    }
                    sessions.add(session);
                    return session;
                }
            });

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

    /**
     * Close all sessions that are still alive and shut down
     * {@link BusSessionFactory} which created all sessions for this instance.
     */
    @Override
    public void close() {
        sessions.forEach(AbstractSession::close);
        factory.close();
    }

    /**
     * Create new {@link SubscriberSession}.
     *
     * @param uri URI of remote endpoint where publisher is listening
     * @param handler instance of {@link NotificationMessageHandler} which will
     *            be invoked for every published message,
     * @return {@link SubscriberSession}
     */
    public SubscriberSession subscriber(String uri, NotificationMessageHandler handler) {
        final SessionKey key = new SessionKey(SessionType.SUB, uri, handler);
        final SubscriberSessionImpl session = (SubscriberSessionImpl) sessionCache.getUnchecked(key);
        session.addReference();
        return session;
    }

    /**
     * Create {@link PublisherSession} bound to given endpoint. If there was
     * previous attempt to obtain publisher on same endpoint, cached instance is
     * returned instead (unless it was closed already).
     *
     * @param uri endpoint to bound to
     * @return {@link PublisherSession}.
     */
    public PublisherSession publisher(String uri) {
        final SessionKey key = new SessionKey(SessionType.PUB, uri, SessionKey.NOOP_HANDLER);
        final PublisherSessionImpl session = (PublisherSessionImpl) sessionCache.getUnchecked(key);
        session.addReference();
        return session;
    }

    /**
     * Create {@link RequesterSession} against remote peer at given URI. If such
     * session already exists in cache (that is against same remote endpoint and
     * same handler) it is returned instead.
     *
     * @param uri URI of remote responder.
     * @param handler {@link ReplyMessageHandler} to be invoked on response
     * @return {@link RequesterSession}
     */
    public RequesterSession requester(String uri, ReplyMessageHandler handler) {
        final SessionKey key = new SessionKey(SessionType.REQ, uri, handler);
        final RequesterSessionImpl session = (RequesterSessionImpl) sessionCache.getUnchecked(key);
        session.addReference();
        return session;
    }

    /**
     * Create {@link ResponderSession} bound to given endpoint. If such session
     * already exists in cache (that is with same endpoint and handler) it is
     * returned instead.
     *
     * @param uri URI to bound this instance to
     * @param handler instance of {@link RequestMessageHandler} that will handle
     *            incoming requests
     * @return {@link ResponderSession}
     */
    public ResponderSession responder(String uri, RequestMessageHandler handler) {
        final SessionKey key = new SessionKey(SessionType.REP, uri, handler);
        final ResponderSessionImpl session = (ResponderSessionImpl) sessionCache.getUnchecked(key);
        session.addReference();
        return session;
    }

    /**
     * This callback method is called from {@link AbstractSession#close()} once
     * reference count of session reach 0.
     */
    @Override
    public void accept(AutoCloseable closeable) {
        sessionCache.asMap().values().removeIf(c -> c == closeable);
        sessions.remove(closeable);
    }

    @VisibleForTesting
    int getSessionCount() {
        return sessions.size();
    }
}
