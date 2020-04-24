/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import java.util.Map;

/**
 * Various transport specific constants.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 26, 2018
 */
@SuppressWarnings({ "squid:S1313"})
public final class Constants {
    public static final int MESSAGE_SIZE = 256 * 1024;
    public static final String SW_ID = "JSON-RPC 2.0";
    public static final String USER_AGENT = SW_ID + " client";
    public static final String SERVER_SW = SW_ID + " server";
    public static final String HANDLER_SSL = "ssl";
    public static final String HANDLER_WRAPPER = "http-wrapper";
    public static final String HANDLER_WS_HANDSHAKE = "ws-handshake";
    public static final String HANDLER_WS_HANDSHAKE_LISTENER = "ws-handshake-listener";
    public static final String HANDLER_AGGREGATOR = "http-aggregator";
    public static final String HANDLER_AUTH = "auth-handler";
    public static final String HANDLER_CLIENT = "client-handler";

    /**
     * {@link Attribute} which holds parsed URI parameters used to bootstrap
     * this {@link Channel}.
     */
    public static final AttributeKey<Map<String, String>> ATTR_URI_OPTIONS = AttributeKey.valueOf(Constants.class,
            "URI_OPTIONS");

    private Constants() {
        // no instantiation
    }
}
