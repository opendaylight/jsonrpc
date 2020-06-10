/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.Maps;
import com.google.common.reflect.Reflection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactoryProvider;
import org.opendaylight.jsonrpc.bus.messagelib.AbstractTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.NoopReplyMessageHandler;
import org.opendaylight.jsonrpc.bus.messagelib.RequesterSession;
import org.opendaylight.jsonrpc.bus.messagelib.ResponderSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.bus.spi.EventLoopConfiguration;
import org.opendaylight.jsonrpc.bus.spi.EventLoopGroupProvider;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link TransportFactory} capable of (de)serializing binding-generated objects.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 20, 2018
 */
public final class SchemaAwareTransportFactory extends AbstractTransportFactory {
    private static final Logger LOG = LoggerFactory.getLogger(SchemaAwareTransportFactory.class);
    private final Map<Object, ProxyContext<? extends RpcService>> proxyMap = Maps.newConcurrentMap();
    private final RpcInvocationAdapter invocationAdapter;

    /**
     * This constructor is not meant to be called directly, but via {@link Builder#build()}. Created instance is
     * suitable in embedded applications.
     *
     * @param invocationAdapter instance of {@link RpcInvocationAdapter} used to process RPC
     * @param eventLoopConfiguration {@link EventLoopConfiguration}
     */
    private SchemaAwareTransportFactory(final RpcInvocationAdapter invocationAdapter,
            EventLoopConfiguration eventLoopConfiguration) {
        super(new EmbeddedBusSessionFactoryProvider(eventLoopConfiguration));
        this.invocationAdapter = Objects.requireNonNull(invocationAdapter);
    }

    /**
     * This constructor is meant to be used within controller context, primary in blueprint definition.
     *
     * @param invocationAdapter instance of {@link RpcInvocationAdapter} used to process RPC
     * @param busSessionFactoryProvider custom {@link BusSessionFactoryProvider} instance
     */
    public SchemaAwareTransportFactory(final ControllerRpcInvocationAdapter invocationAdapter,
            BusSessionFactoryProvider busSessionFactoryProvider) {
        super(busSessionFactoryProvider);
        this.invocationAdapter = Objects.requireNonNull(invocationAdapter);
    }

    @SuppressWarnings({ "checkstyle:HiddenField", "squid:S2176" })
    public static class Builder implements org.opendaylight.yangtools.concepts.Builder<SchemaAwareTransportFactory> {
        private EventLoopConfiguration eventLoopConfiguration;
        private RpcInvocationAdapter rpcInvocationAdapter;

        public Builder withRpcInvocationAdapter(RpcInvocationAdapter rpcInvocationAdapter) {
            this.rpcInvocationAdapter = rpcInvocationAdapter;
            return this;
        }

        public Builder withEventLoopConfig(EventLoopConfiguration eventLoopConfiguration) {
            this.eventLoopConfiguration = eventLoopConfiguration;
            return this;
        }

        @Override
        public SchemaAwareTransportFactory build() {
            if (eventLoopConfiguration == null) {
                eventLoopConfiguration = EventLoopGroupProvider.config();
            }
            if (rpcInvocationAdapter == null) {
                rpcInvocationAdapter = EmbeddedRpcInvocationAdapter.INSTANCE;
            }
            return new SchemaAwareTransportFactory(rpcInvocationAdapter, eventLoopConfiguration);
        }
    }

    /**
     * Create requester session against remote endpoint by creating proxy of given type.
     *
     * @param type Type of {@link RpcService} to create proxy for
     * @param uri remote endpoint URI
     * @return proxy for {@link RpcService}.
     * @throws URISyntaxException if provided URI is invalid
     */
    public <T extends RpcService> ProxyContext<T> createBindingRequesterProxy(Class<T> type, String uri)
            throws URISyntaxException {
        LOG.info("Creating requester proxy for type {} against endpoint '{}'", type.getName(), uri);
        final RequesterSession requester = createRequester(uri, NoopReplyMessageHandler.INSTANCE);
        final OutboundHandler<T> handler = new OutboundHandler<>(type, invocationAdapter, requester);
        final T proxy = Reflection.newProxy(type, handler);
        final ObjectRegistration<T> rpcReg = invocationAdapter.registerImpl(type, proxy);
        final ProxyContext<T> context = new ProxyContext<>(type, rpcReg, requester, proxy, this::closeProxy);
        proxyMap.put(proxy, context);
        return context;
    }

    /**
     * Create binding-aware requester proxy against remote responder endpoint implementing multiple service models.
     *
     * @param services set of {@link RpcService} implemented by remote service
     * @param uri remote responder endpoint
     * @return {@link MultiModelProxy} used to get actual RPC proxy and close requester once it is no longer needed
     * @throws URISyntaxException if provided responder URI is invalid
     */
    @SuppressWarnings("unchecked")
    public MultiModelProxy createMultiModelRequesterProxy(Set<Class<? extends RpcService>> services, String uri)
            throws URISyntaxException {
        final Set<ProxyContext<RpcService>> proxies = new HashSet<>();
        for (final Class<? extends RpcService> service : services) {
            proxies.add((ProxyContext<RpcService>) createBindingRequesterProxy(service, uri));
        }
        return new MultiModelProxy(proxies);
    }

    /**
     * Closes proxy instance created by {@link #createRequesterProxy(Class, String)}.
     *
     * @param proxy proxy instance to close.
     */
    private void closeProxy(Object proxy) {
        final ProxyContext<? extends RpcService> context = proxyMap.remove(proxy);
        if (context != null) {
            context.closeInternal();
        }
    }

    /**
     * Create responder bound to local socket provided in URI.
     *
     * @param type actual {@link RpcService} type
     * @param rpcImpl {@link RpcService} implementation
     * @param bindUri URI to bind to
     * @return {@link ResponderSession}
     * @throws URISyntaxException if provided URI is invalid
     */
    public <T extends RpcService> ResponderSession createResponder(Class<T> type, T rpcImpl, String bindUri)
            throws URISyntaxException {
        LOG.info("Creating responder type {} exposed on '{}'", type.getName(), bindUri);
        final URI uri = new URI(bindUri);
        return getMessageLibraryForTransport(uri.getScheme()).responder(bindUri,
                new InboundHandler<>(type, invocationAdapter, rpcImpl), false);
    }

    @SuppressWarnings("unchecked")
    public ResponderSession createMultiModelResponder(MultiModelBuilder builder, String bindUri)
            throws URISyntaxException {
        final ClassToInstanceMap<RpcService> services = builder.build();

        LOG.info("Creating multi-model responder for services {} exposed on '{}'", services.keySet(), bindUri);
        final URI uri = new URI(bindUri);

        final Set<InboundHandler<RpcService>> handlers = services.entrySet()
                .stream()
                .map(e -> new InboundHandler<>((Class<RpcService>) e.getKey(), invocationAdapter, e.getValue()))
                .collect(Collectors.toSet());

        return getMessageLibraryForTransport(uri.getScheme()).responder(bindUri,
                new MultiModelRequestDispatcher(handlers), false);
    }

    /**
     * Perform cleanup. Every proxy instance created by this {@link TransportFactory} is guaranteed to be closed once
     * call to this method finished successfully.
     */
    @Override
    public void close() {
        proxyMap.values().stream().forEach(ProxyContext::close);
        proxyMap.clear();
        super.close();
    }
}
