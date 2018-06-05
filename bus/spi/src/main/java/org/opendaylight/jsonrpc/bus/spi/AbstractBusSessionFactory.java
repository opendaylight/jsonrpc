/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.jsonrpc.bus.api.BusSession;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.security.api.SecurityService;
import org.opendaylight.jsonrpc.security.noop.NoopSecurityService;

/**
 * Common code for {@link BusSessionFactory} implementations.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 6, 2018
 */
@SuppressFBWarnings({ "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE" })
public abstract class AbstractBusSessionFactory implements BusSessionFactory {
    protected final String name;
    protected final ServerBootstrap serverBootstrap;
    protected final Bootstrap clientBootstrap;
    protected final EventExecutorGroup handlerExecutor;
    protected final Set<WeakReference<BusSession>> sessions = ConcurrentHashMap.newKeySet();
    protected final SecurityService securityService;

    public AbstractBusSessionFactory(final String name) {
        this(name, EventLoopGroupProvider.config(), NoopSecurityService.INSTANCE);
    }

    public AbstractBusSessionFactory(final String name, final EventLoopConfiguration config) {
        this(name, config, NoopSecurityService.INSTANCE);
    }

    public AbstractBusSessionFactory(final String name, final EventLoopConfiguration config,
            final SecurityService securityService) {
        this.name = name;
        serverBootstrap = new ServerBootstrap().channel(NioServerSocketChannel.class).group(config.bossGroup(),
                config.workerGroup());
        clientBootstrap = new Bootstrap().channel(NioSocketChannel.class).group(config.workerGroup());
        this.handlerExecutor = config.handlerGroup();
        this.securityService = securityService;
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Add {@link BusSession} into set of created sessions so that it can be cleaned at shutdown.
     *
     * @param session session to add to set.
     */
    protected void addSession(BusSession session) {
        sessions.add(new WeakReference<>(session));
    }

    @Override
    public void close() {
        sessions.stream().filter(sessRef -> sessRef.get() != null).forEach(sess -> sess.get().close());
    }

    /**
     * Helper method to get {@link URI} from string specification.
     *
     * @param uriStr raw uri
     * @return {@link URI}
     */
    protected static URI createUriUnchecked(String uriStr) {
        try {
            return new URI(uriStr);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(String.format("Invalid URI : %s", uriStr), e);
        }
    }
}
