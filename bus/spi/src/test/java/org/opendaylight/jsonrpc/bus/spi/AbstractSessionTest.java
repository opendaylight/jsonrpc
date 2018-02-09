/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.escape.Escaper;
import com.google.common.io.Resources;
import com.google.common.net.UrlEscapers;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;

/**
 * Base endpoint test class. It is meant to be extended by unit tests in
 * transport implementations.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 7, 2018
 */
public abstract class AbstractSessionTest {
    protected EventLoopGroup group;
    protected BusSessionFactory factory;
    private final Collection<Path> toDelete = new HashSet<>();

    @Before
    public void setUp() {
        group = new NioEventLoopGroup(10);
        factory = createFactory();
    }

    protected abstract BusSessionFactory createFactory();

    @After
    public void tearDown() {
        toDelete.stream().forEach(t -> {
            try {
                Files.deleteIfExists(t);
            } catch (IOException e) {
                // Ignore, there is nothing to do
            }
        });
        factory.close();
        group.shutdownGracefully();
    }

    /**
     * Copy resource from classpath to temporary location. File is deleted
     * during teardown.
     *
     * @param name name of resource to copy
     * @return path to copied resource
     */
    protected String copyResource(String name) throws IOException {
        final Path path = Files.createTempFile("jsonrpc", "test");
        Resources.copy(getClass().getResource(name), Files.newOutputStream(path));
        toDelete.add(path);
        return path.toString();
    }

    protected String getBindUri(int port) {
        return String.format("%s://0.0.0.0:%d", factory.name(), port);
    }

    protected String getConnectUri(int port) {
        return String.format("%s://127.0.0.1:%d", factory.name(), port);
    }

    protected static class UriBuilder {
        Escaper escaper = UrlEscapers.urlFormParameterEscaper();
        private final String base;
        private final Map<String, String> params = new LinkedHashMap<>();
        private static final MapJoiner PARAM_JOINER = Joiner.on('&').withKeyValueSeparator('=');

        public UriBuilder(String base) {
            this.base = base;
        }

        public UriBuilder add(String name, String value) {
            params.put(escaper.escape(name), escaper.escape(value));
            return this;
        }

        public String build() {
            final StringBuilder sb = new StringBuilder();
            sb.append(base);
            sb.append('?');
            sb.append(PARAM_JOINER.join(params));
            return sb.toString();
        }
    }

    public static int getFreeTcpPort() {
        int port = -1;
        try {
            final Socket socket = new Socket();
            socket.bind(null);
            port = socket.getLocalPort();
            socket.close();
            return port;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
