/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import com.google.common.net.InetAddresses;

import io.netty.channel.ChannelFuture;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import org.opendaylight.jsonrpc.bus.api.BusSession;
import org.opendaylight.jsonrpc.bus.api.SessionType;

/**
 * Common class of all session types.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 7, 2018
 */
public abstract class AbstractSession implements AutoCloseable, BusSession {
    protected volatile ChannelFuture channelFuture;
    protected final InetSocketAddress address;
    protected final URI uri;
    protected final SessionType sessionType;

    public AbstractSession(String uriStr, int defaultPort, SessionType sessionType) {
        uri = getUriSafe(uriStr);
        address = addressFromUri(uri, defaultPort);
        this.sessionType = Objects.requireNonNull(sessionType);
    }

    @Override
    public void close() {
        if (channelFuture != null) {
            channelFuture.channel().close().syncUninterruptibly();
        }
    }

    @Override
    public SessionType getSessionType() {
        return sessionType;
    }

    /**
     * Helper method to create {@link InetSocketAddress} from {@link URI}
     * specification.
     *
     * @param uri {@link URI} from which to extract {@link InetSocketAddress}
     * @param defaultPort port value to use if omitted in specification
     * @return {@link InetSocketAddress}
     */
    protected static InetSocketAddress addressFromUri(URI uri, int defaultPort) {
        final int port = uri.getPort() == -1 ? defaultPort : uri.getPort();
        return (uri.getHost() == null) ? new InetSocketAddress(InetAddresses.fromInteger(0), port)
                : new InetSocketAddress(uri.getHost(), port);
    }

    /**
     * Get {@link URI} from string specification.
     *
     * @param uriStr raw URI string
     * @return {@link URI}
     */
    protected static URI getUriSafe(String uriStr) {
        try {
            return new URI(uriStr);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(String.format("Invalid URI : '%s'", uriStr), e);
        }
    }
}
