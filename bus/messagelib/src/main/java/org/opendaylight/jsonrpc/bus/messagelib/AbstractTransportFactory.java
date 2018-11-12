/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.opendaylight.jsonrpc.bus.api.BusSessionFactoryProvider;
import org.opendaylight.jsonrpc.bus.messagelib.EndpointBuilders.EndpointBuilder;

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
    public <T extends AutoCloseable> T createPublisherProxy(Class<T> clazz, String rawUri, boolean skipCache)
            throws URISyntaxException {
        final URI uri = new URI(rawUri);
        ensureTransport(uri);
        final MessageLibrary messageLibrary = getMessageLibraryForTransport(uri.getScheme());
        final ProxyService proxy = proxyCache.getUnchecked(messageLibrary);
        return proxy.createPublisherProxy(uri.toString(), clazz, skipCache);
    }

    @Override
    public <T extends AutoCloseable> T createPublisherProxy(Class<T> clazz, String rawUri)
            throws URISyntaxException {
        return createPublisherProxy(clazz, rawUri, true);
    }

    @Override
    public <T extends AutoCloseable> T createRequesterProxy(Class<T> clazz, String rawUri)
            throws URISyntaxException {
        return createRequesterProxy(clazz, rawUri, true);
    }

    @Override
    public <T extends AutoCloseable> T createRequesterProxy(Class<T> clazz, String rawUri, boolean skipCache)
            throws URISyntaxException {
        final URI uri = new URI(rawUri);
        ensureTransport(uri);
        final MessageLibrary messageLibrary = getMessageLibraryForTransport(uri.getScheme());
        final ProxyService proxy = proxyCache.getUnchecked(messageLibrary);
        return proxy.createRequesterProxy(uri.toString(), clazz, skipCache);
    }

    @Override
    public <T extends AutoCloseable> ResponderSession createResponder(String rawUri, T handler)
            throws URISyntaxException {
        return createResponder(rawUri, handler, true);
    }

    @Override
    public <T extends AutoCloseable> ResponderSession createResponder(String rawUri, T handler, boolean skipCache)
            throws URISyntaxException {
        final URI uri = new URI(rawUri);
        ensureTransport(uri);
        return getMessageLibraryForTransport(uri.getScheme()).responder(rawUri, new ResponderHandlerAdapter(handler),
                skipCache);
    }

    @Override
    public <T extends AutoCloseable> SubscriberSession createSubscriber(String rawUri, T handler)
            throws URISyntaxException {
        return createSubscriber(rawUri, handler, true);
    }

    @Override
    public <T extends AutoCloseable> SubscriberSession createSubscriber(String rawUri, T handler, boolean skipCache)
            throws URISyntaxException {
        final URI uri = new URI(rawUri);
        ensureTransport(uri);
        return getMessageLibraryForTransport(uri.getScheme()).subscriber(rawUri, new SubscriberHandlerAdapter(handler),
                skipCache);
    }

    @Override
    public RequesterSession createRequester(String rawUri, ReplyMessageHandler handler) throws URISyntaxException {
        return createRequester(rawUri, handler, true);
    }

    @Override
    public RequesterSession createRequester(String rawUri, ReplyMessageHandler handler, boolean skipCache)
            throws URISyntaxException {
        final URI uri = new URI(rawUri);
        ensureTransport(uri);
        return getMessageLibraryForTransport(uri.getScheme()).requester(rawUri, handler, skipCache);
    }

    @Override
    public void close() {
        // All loaded MessageLibrary instances will be closed at this point
        messageLibraryCache.cleanUp();
        proxyCache.cleanUp();
    }

    @Override
    public MessageLibrary getMessageLibraryForTransport(String transport) {
        return messageLibraryCache.getUnchecked(transport);
    }

    @Override
    public EndpointBuilder endpointBuilder() {
        return new EndpointBuilders.EndpointBuilder(this);
    }

    @Override
    public boolean isClientConnected(Object proxyOrSession) {
        if (Proxy.isProxyClass(proxyOrSession.getClass())) {
            final Optional<BaseSession> session = proxyCache.asMap()
                    .values()
                    .stream()
                    .map(x -> x.getProxySession(proxyOrSession))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
            return session.isPresent() && (session.get() instanceof ClientSession)
                    && ((ClientSession) session.get()).isConnectionReady();
        } else {
            return (proxyOrSession instanceof ClientSession) && ((ClientSession) proxyOrSession).isConnectionReady();
        }
    }

    private static void ensureTransport(URI uri) {
        Preconditions.checkArgument(uri.getScheme() != null, "Transport is required, but not provided in URI : %s",
                uri);
    }
}
