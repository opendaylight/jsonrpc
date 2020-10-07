/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.io.Resources;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactoryProvider;
import org.opendaylight.jsonrpc.bus.messagelib.TcclBusSessionFactoryProvider;
import org.opendaylight.jsonrpc.bus.spi.EventLoopConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link BusSessionFactoryProvider} which uses similar approach like {@link TcclBusSessionFactoryProvider}, but it
 * call custom constructor.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Oct 14, 2018
 */
public class EmbeddedBusSessionFactoryProvider implements BusSessionFactoryProvider {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedBusSessionFactoryProvider.class);
    private static final String CP_RESOURCE = "META-INF/services/" + BusSessionFactory.class.getName();
    private static final Splitter LINE_SPLITTER = Splitter.on(Pattern.compile("\r\n|\n|\r"));
    private static final Set<Class<?>> FACTORY_CLASSES = new HashSet<>();
    private final ImmutableSet<BusSessionFactory> factories;

    static {
        try {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            final Enumeration<URL> candidates = cl.getResources(CP_RESOURCE);
            while (candidates.hasMoreElements()) {
                final URL current = candidates.nextElement();
                LOG.debug("Loading BusSessionFactories from {}", current);
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Resources.copy(current, baos);
                final List<String> classes = LINE_SPLITTER.splitToList(baos.toString(StandardCharsets.UTF_8))
                        .stream()
                        .filter(s -> !"".equals(s.trim()))
                        .filter(s -> !s.trim().startsWith("#"))
                        .collect(Collectors.toList());
                classes.iterator().forEachRemaining(c -> {
                    try {
                        LOG.debug("Attempting to load '{}'", c);
                        FACTORY_CLASSES.add(Class.forName(c, true, cl));
                    } catch (ClassNotFoundException e) {
                        LOG.warn("Unable to load '{}', skipping this factory", c);
                    }
                });
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public EmbeddedBusSessionFactoryProvider(EventLoopConfiguration config) {
        final Builder<BusSessionFactory> builder = ImmutableSet.builder();
        FACTORY_CLASSES.stream().forEach(c -> {
            LOG.debug("Attempting to instantiate '{}'", c.getName());
            try {
                final BusSessionFactory instance = (BusSessionFactory) c
                        .getDeclaredConstructor(EventLoopConfiguration.class).newInstance(config);
                builder.add(instance);
            } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
                LOG.warn("Unable to instantiate '{}', factory skipped", c.getName(), e);
            }
        });
        factories = builder.build();
    }

    @Override
    public Iterator<BusSessionFactory> getBusSessionFactories() {
        return factories.iterator();
    }
}
