/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.security.api;

import com.google.common.base.Splitter;
import io.netty.handler.ssl.ClientAuth;
import java.security.KeyStore;
import java.security.PrivateKey;
import javax.net.ssl.KeyManagerFactory;

/**
 * Security related constants and URI tokens.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jun 8, 2018
 */
@SuppressWarnings("squid:S2068")
public final class SecurityConstants {
    static final Splitter COMMA_SPLITTER = Splitter.on(',');
    /**
     * Comma-separated list of allowed ciphers. If not specified, JVM defaults are used
     */
    public static final String OPT_CIPHERS = "ciphers";

    /**
     * Comma-separated list of allowed protocols. If not specified, JVM defaults are used.
     */
    public static final String OPT_PROTOCOLS = "protocols";

    /**
     * Type of KeyStore to use. Currently "JKS" and "PKCS12" is supported. If not specified, then value of
     * {@link #KEYSTORE_TYPE_DEFAULT} will be used.
     */
    public static final String OPT_KEYSTORE_TYPE = "keystore-type";

    /**
     * JKS {@link KeyStore} type.
     */
    public static final String KEYSTORE_TYPE_JKS = "JKS";

    /**
     * PKCS12 {@link KeyStore} type.
     */
    public static final String KEYSTORE_TYPE_PKCS12 = "PKCS12";

    /**
     * Default {@link KeyStore} type to use when {@link #OPT_KEYSTORE_TYPE} is not specified.
     */
    public static final String KEYSTORE_TYPE_DEFAULT = KEYSTORE_TYPE_JKS;

    /**
     * Password used to unlock {@link KeyStore}. Mandatory presence of this option depends upon used
     * {@link KeyStoreFactory} used.
     */
    public static final String OPT_KEYSTORE_PASSWORD = "keystore-password";

    /**
     * Path to {@link KeyStore} containing {@link PrivateKey}.
     */
    public static final String OPT_KEYSTORE_FILE = "keystore-file";

    /**
     * Password used to unlock CA trust {@link KeyStore}. Mandatory presence of this option depends upon
     * {@link KeyStoreFactory} used.
     */
    public static final String OPT_TRUSTSTORE_PASSWORD = "truststore-password";

    /**
     * Path to file containing CA trust material.
     */
    public static final String OPT_TRUSTSTORE_FILE = "truststore-file";

    /**
     * Certificate verification level for the Client Authentication. If not specified, then
     * {@link #TLS_CLIENT_VERIFY_DEFAULT} is used. Possible values are "NONE", "OPTIONAL", "REQUIRE".
     */
    public static final String OPT_CLIENT_VERIFY = "client-verify";

    /**
     * Default value of client certificate verification.
     */
    public static final String TLS_CLIENT_VERIFY_DEFAULT = ClientAuth.NONE.name();

    /**
     * Default value of max permitted CA trust length.
     */
    public static final String TLS_VERIFY_DEPTH_DEFAULT = String.valueOf(3);

    /**
     * Max permitted CA trust length.
     */
    public static final String OPT_TLS_VERIFY_DEPTH = "tls-verify-depth";

    /**
     * Sun's X509 {@link KeyManagerFactory} name.
     */
    public static final String KMF_SUN_X509 = "SunX509";

    /**
     * Certificate validation : strict, ignore.
     */
    public static final String OPT_CERT_POLICY = "cert-validation-policy";

    /**
     * Trust any X.509 certificate.
     */
    public static final String CERT_POLICY_IGNORE = "ignore";

    /**
     * Trust only valid X.509 certificate.
     */
    public static final String CERT_POLICY_STRICT = "strict";

    /**
     * Default certificate validation policy.
     */
    public static final String CERT_POLICY_DEFAULT = CERT_POLICY_STRICT;

    /**
     * When present on client endpoint, username and password must be present and will be sent as basic authentication
     * header. When present on server endpoint, request headers are inspected and validated against realm using
     * AuthenticationProvider.
     */
    public static final String OPT_REQ_AUTH = "auth";

    /**
     * Client-provided username.
     */
    public static final String OPT_USERNAME = "auth-username";

    /**
     * Client-provided password.
     */
    public static final String OPT_PASSWORD = "auth-password";

    /**
     * Number of certificates in chain that is allowed while verifying client certificate.
     */
    public static final String OPT_CLIENT_CHAIN_LENGTH = "client-chain-max-length";

    /**
     * Number of certificates in chain that is allowed while verifying server certificate.
     */
    public static final String OPT_SERVER_CHAIN_LENGTH = "server-chain-max-length";

    /**
     * Certificate alias.
     */
    public static final String OPT_CERT_ALIAS = "certificate-alias";

    private SecurityConstants() {
        // prevent others to create instances of this class
    }
}
