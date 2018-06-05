/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.ProgressivePromise;
import io.netty.util.internal.SystemPropertyUtil;

import java.util.concurrent.atomic.AtomicReference;

import org.opendaylight.jsonrpc.bus.api.PeerContext;
import org.opendaylight.jsonrpc.bus.api.SessionType;

/**
 * Common constants shared across all transport implementations.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 7, 2018
 */
public final class CommonConstants {
    public static final boolean DEBUG_MODE;
    /**
     * {@link LoggingHandler} is {@link Sharable}, so it is possible to reuse single instance.
     */
    public static final LoggingHandler LOG_HANDLER;

    static {
        DEBUG_MODE = SystemPropertyUtil.getBoolean("org.opendaylight.jsonrpc.bus.debug", false);
        LOG_HANDLER = new LoggingHandler(LogLevel.INFO);
    }

    private CommonConstants() {
        // no instantiation here
    }

    /**
     * Attribute to hold reference to response queue.
     */
    public static final AttributeKey<AtomicReference<ProgressivePromise<String>>> ATTR_RESPONSE_QUEUE = AttributeKey
            .valueOf(CommonConstants.class, "responseQueue");

    /**
     * Flag to indicate that protocol handshake is done.
     */
    public static final AttributeKey<Boolean> ATTR_HANDSHAKE_DONE = AttributeKey.valueOf(CommonConstants.class,
            "HANDSHAKE_DONE");
    /**
     * {@link Channel}'s {@link SessionType}.
     */
    public static final AttributeKey<SessionType> ATTR_SOCKET_TYPE = AttributeKey.valueOf(CommonConstants.class,
            "SOCKET_TYPE");

    public static final AttributeKey<PeerContext> ATTR_PEER_CONTEXT = AttributeKey.valueOf(CommonConstants.class,
            "PEER_CONTEXT");
    /**
     * Attribute to hold negotiated SSL session parameters.
     */
    public static final AttributeKey<SslSessionInfo> ATTR_SSL_INFO = AttributeKey.valueOf(CommonConstants.class,
            "SSL_INFO");
    /**
     * Attribute to hold authentication parameters.
     */
    public static final AttributeKey<ChannelAuthentication> ATTR_AUTH_INFO = AttributeKey.valueOf(CommonConstants.class,
            "AUTH_INFO");
    public static final String HANDLER_CONN_TRACKER = "conn-tracker";
    public static final String HANDLER_LOGGING = "logging";
    public static final String HANDLER_LISTENER = "listener-adapter";
    public static final String HANDLER_CODEC = "codec";
    public static final String HANDLER_SSL_INFO = "ssl-info";
}
