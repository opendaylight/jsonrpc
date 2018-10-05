/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
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
import com.google.common.cache.RemovalListener;

import java.net.URI;
import java.net.URISyntaxException;

import org.opendaylight.jsonrpc.bus.api.BusSessionFactoryProvider;

/**
 * Abstract base for a TransportFactory.
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractTransportFactory implements TransportFactory {
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
        messageLibraryCache = CacheBuilder.newBuilder()
                .weakValues()
                .removalListener((RemovalListener<String, MessageLibrary>) e -> e.getValue().close())
                .build(new CacheLoader<String, MessageLibrary>() {
                    @Override
                    public MessageLibrary load(String key) throws Exception {
                        return new MessageLibrary(busSessionFactoryProvider, key);
                    }
                });
    }

    @Override
    public <T extends AutoCloseable> T createPublisherProxy(Class<T> clazz, String rawUri)
            throws URISyntaxException {
        final URI uri = new URI(rawUri);
        final MessageLibrary messageLibrary = messageLibraryForTransport(uri.getScheme());
        final ProxyService proxy = proxyCache.getUnchecked(messageLibrary);
        return proxy.createPublisherProxy(uri.toString(), clazz);
    }

    @Override
    public <T extends AutoCloseable> T createRequesterProxy(Class<T> clazz, String rawUri)
            throws URISyntaxException {
        final URI uri = new URI(rawUri);
        final MessageLibrary messageLibrary = messageLibraryForTransport(uri.getScheme());
        final ProxyService proxy = proxyCache.getUnchecked(messageLibrary);
        return proxy.createRequesterProxy(uri.toString(), clazz);
    }

    @Override
    public <T extends AutoCloseable> ResponderSession createResponder(String rawUri, T handler)
            throws URISyntaxException {
        final URI uri = new URI(rawUri);
        return messageLibraryForTransport(uri.getScheme()).responder(rawUri, new ResponderHandlerAdapter(handler));
    }

    @Override
    public <T extends AutoCloseable> SubscriberSession createSubscriber(String rawUri, T handler)
            throws URISyntaxException {
        final URI uri = new URI(rawUri);
        return messageLibraryForTransport(uri.getScheme()).subscriber(rawUri, new SubscriberHandlerAdapter(handler));
    }

    @Override
    public RequesterSession createRequester(String rawUri, ReplyMessageHandler handler) throws URISyntaxException {
        final URI uri = new URI(rawUri);
        return messageLibraryForTransport(uri.getScheme()).requester(rawUri, handler);
    }

    @Override
    public void close() {
        // All loaded MessageLibrary instances will be closed at this point
        messageLibraryCache.cleanUp();
        proxyCache.cleanUp();
    }

    @VisibleForTesting
    public MessageLibrary messageLibraryForTransport(String transport) {
        return messageLibraryCache.getUnchecked(transport);
    }
}
