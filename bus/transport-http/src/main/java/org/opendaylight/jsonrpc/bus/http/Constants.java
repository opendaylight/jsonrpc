/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

/**
 * Various transport specific constants.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 26, 2018
 */
@SuppressWarnings({ "squid:S1313", "squid:S2068" })
public final class Constants {
    public static final String INADDR_ANY = "0.0.0.0";
    public static final String WILDCARD = "*";
    public static final int DEFAULT_TIMEOUT = 30 * 1000; // 30 seconds
    public static final int RECONNECT_INTERVAL = 500; // 0.5 second
    public static final int MAX_THREADS = 8;
    public static final String SW_ID = "JSON-RPC 2.0";
    public static final String USER_AGENT = SW_ID + " client";
    public static final String SERVER_SW = SW_ID + " server";
    public static final String HANDLER_SSL = "ssl";
    public static final String HANDLER_WRAPPER = "http-wrapper";
    public static final String HANDLER_WS_HANDSHAKE = "ws-handshake";
    public static final String HANDLER_WS_HANDSHAKE_LISTENER = "ws-handshake-listener";
    public static final String HANDLER_AGGREGATOR = "http-aggregator";
    public static final String OPT_CIPHERS = "ciphers";
    public static final String OPT_KEYSTORE = "keystore";
    public static final String DEFAULT_KEYSTORE = "PKCS12";
    public static final String OPT_KEY_MANAGER_FACTORY = "kmf";
    public static final String DEFAULT_KEY_MANAGER_FACTORY = "SunX509";
    public static final String OPT_PRIVATE_KEY_PASSWORD = "privatekeypassword";
    public static final String OPT_CERT_FILE = "certfile";
    public static final String OPT_PRIVATE_KEY_FILE = "privatekeyfile";
    public static final int MESSAGE_SIZE = 256 * 1024;
    public static final String OPT_CERT_TRUST = "certificatetrustpolicy";
    public static final String DEFAULT_CERT_POLICY = "ignore";
    public static final String HANDLER_CLIENT = "client-handler";

    private Constants() {
        // no instantiation
    }
}
