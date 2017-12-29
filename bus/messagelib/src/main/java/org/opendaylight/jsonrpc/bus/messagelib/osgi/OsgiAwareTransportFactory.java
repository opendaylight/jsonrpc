/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib.osgi;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.net.URISyntaxException;
import java.util.Objects;
import org.opendaylight.jsonrpc.bus.messagelib.MessageLibrary;
import org.opendaylight.jsonrpc.bus.messagelib.Session;
import org.opendaylight.jsonrpc.bus.messagelib.ThreadedSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.Util;
import org.opendaylight.jsonrpc.bus.spi.BusSessionFactoryProvider;

/**
 * Implementation of {@link TransportFactory} which requires semantics of
 * {@link OsgiBusSessionFactoryProvider}.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class OsgiAwareTransportFactory implements TransportFactory, AutoCloseable {
    private BusSessionFactoryProvider busSessionFactoryProvider;
    // need to use weak-values to allow GC of unloaded instances
    private LoadingCache<String, MessageLibrary> cache;

    public void init() {
        Objects.requireNonNull(busSessionFactoryProvider, "BusSessionFactoryProvider was not set");
        cache = CacheBuilder.newBuilder().weakValues().build(new CacheLoader<String, MessageLibrary>() {
            @Override
            public MessageLibrary load(String key) throws Exception {
                return new MessageLibrary(busSessionFactoryProvider, key);
            }
        });
    }

    @Override
    public void close() {
        // All loaded messageLibrary instances will be closed at this point
        cache.asMap().values().stream().forEach(MessageLibrary::close);
        Util.close();
    }

    @Override
    public <T extends AutoCloseable> T createProxy(Class<T> clazz, String rawUri) throws URISyntaxException {
        return Util.createProxy(cache, clazz, rawUri, Util.DEFAULT_TIMEOUT);
    }

    @Override
    public <T extends AutoCloseable> ThreadedSession createResponder(String rawUri, T handler)
            throws URISyntaxException {
        return Util.createThreadedResponderSession(cache, rawUri, handler);
    }

    @Override
    public <T extends AutoCloseable> ThreadedSession createSubscriber(String rawUri, T handler)
            throws URISyntaxException {
        return Util.createThreadedSubscriberSession(cache, rawUri, handler);
    }

    @Override
    public Session createSession(String rawUri) throws URISyntaxException {
        return Util.openSession(cache, rawUri, null);
    }

    public void setBusSessionFactoryProvider(BusSessionFactoryProvider busSessionFactoryProvider) {
        this.busSessionFactoryProvider = busSessionFactoryProvider;
    }
}
