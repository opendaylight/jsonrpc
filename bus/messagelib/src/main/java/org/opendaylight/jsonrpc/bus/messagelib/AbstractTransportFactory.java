/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import java.net.URI;
import java.net.URISyntaxException;
import org.opendaylight.jsonrpc.bus.spi.BusSessionFactoryProvider;

/**
 * Abstract base for a TransportFactory.
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractTransportFactory implements TransportFactory, AutoCloseable {
    private static final int DEFAULT_TIMEOUT = 30000;

    private final LoadingCache<String, MessageLibrary> messageLibraryCache;

    // Cache also proxy instances so they can be reused
    private final LoadingCache<MessageLibrary, ProxyService> proxyCache = CacheBuilder.newBuilder()
        .build(new CacheLoader<MessageLibrary, ProxyService>() {
            @Override
            public ProxyService load(MessageLibrary key) throws Exception {
                return new ProxyServiceImpl(key);
            }
        });

    protected AbstractTransportFactory(BusSessionFactoryProvider busSessionFactoryProvider) {
        messageLibraryCache = CacheBuilder.newBuilder().weakValues().removalListener(
            (RemovalListener<String, MessageLibrary>) notification -> notification.getValue().close())
            .build(new CacheLoader<String, MessageLibrary>() {
                @Override
                public MessageLibrary load(String key) throws Exception {
                    return new MessageLibrary(busSessionFactoryProvider, key);
                }
            });
    }

    @Override
    public void close() {
        // All loaded MessageLibrary instances will be closed at this point
        messageLibraryCache.cleanUp();
    }

    @Override
    public <T extends AutoCloseable> T createProxy(Class<T> clazz, String rawUri) throws URISyntaxException {
        URI uri = new URI(rawUri);
        EndpointRole role = EndpointRole.valueOf(Util.tokenizeQuery(uri.getQuery()).get("role"));
        MessageLibrary messageLibrary = getMessageLibrary(uri);
        ProxyService proxy = proxyCache.getUnchecked(messageLibrary);

        switch (role) {
            case PUB:
                return proxy.createPublisherProxy(Util.prepareUri(uri), clazz, DEFAULT_TIMEOUT);
            case REQ:
                return proxy.createRequesterProxy(Util.prepareUri(uri), clazz, DEFAULT_TIMEOUT);
            default:
                throw new IllegalArgumentException(String.format("Unrecognized endoint role : %s", role));
        }
    }

    @Override
    public <T extends AutoCloseable> ThreadedSession createResponder(String rawUri, T handler)
            throws URISyntaxException {
        URI uri = new URI(rawUri);
        return getMessageLibrary(uri).threadedResponder(Util.prepareUri(uri), handler);
    }

    @Override
    public <T extends AutoCloseable> ThreadedSession createSubscriber(String rawUri, T handler)
            throws URISyntaxException {
        URI uri = new URI(rawUri);
        return getMessageLibrary(uri).threadedSubscriber(Util.prepareUri(uri), handler);
    }

    @Override
    public Session createSession(String rawUri) throws URISyntaxException {
        URI uri = new URI(rawUri);
        return Util.openSession(getMessageLibrary(uri), uri, null);
    }

    private MessageLibrary getMessageLibrary(URI uri) {
        return messageLibraryCache.getUnchecked(uri.getScheme());
    }
}
