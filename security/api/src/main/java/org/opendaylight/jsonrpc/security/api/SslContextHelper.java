/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.security.api;

import static org.opendaylight.jsonrpc.security.api.SecurityConstants.CERT_POLICY_DEFAULT;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.CERT_POLICY_IGNORE;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.COMMA_SPLITTER;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.KEYSTORE_TYPE_DEFAULT;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.KEYSTORE_TYPE_JKS;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.KEYSTORE_TYPE_PKCS12;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.OPT_CERT_POLICY;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.OPT_CIPHERS;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.OPT_CLIENT_VERIFY;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.OPT_KEYSTORE_TYPE;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.OPT_PROTOCOLS;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.TLS_CLIENT_VERIFY_DEFAULT;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import javax.net.ssl.TrustManagerFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for creating {@link SslContext} from URI options.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jun 7, 2018
 */
public final class SslContextHelper {
    private static final Logger LOG = LoggerFactory.getLogger(SslContextHelper.class);

    static {
        if (Security.addProvider(new BouncyCastleProvider()) == -1) {
            LOG.debug("BouncyCastleProvider is already installed");
        }
    }

    private SslContextHelper() {
        // no instantiation of this class
    }

    /**
     * Split string by comma into iterable. If resulting {@link Iterable} is about to be empty, NULL is returned
     * instead.
     *
     * @param options key-value mapping of URI options
     * @param key map key
     * @return {@link Iterable} or strings or NULL
     */
    private static Iterable<String> splitOrNull(Map<String, String> options, String key) {
        final String val = options.getOrDefault(key, "").trim();
        if ("".equals(val)) {
            return null;
        }
        return COMMA_SPLITTER.split(val);
    }

    /**
     * Split string by comma into array. If array is about to be empty, NULL is returned instead.
     *
     * @param options key-value mapping of URI options
     * @param key map key
     * @return array of strings or NULL if array is empty
     *
     * @see SslContextBuilder#protocols(String...) Note: suppressed squid warning S1168 because we purposely need NULL
     *      instead of empty array.
     */
    @SuppressWarnings("squid:S1168")
    @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
    private static String[] splitToArrayOrNull(Map<String, String> options, String key) {
        final String val = options.getOrDefault(key, "").trim();
        if ("".equals(val)) {
            return null;
        }
        return val.split(",");
    }

    /**
     * Create {@link SslContext} for client connection.
     *
     * @param options key-value map of URII options
     * @return {@link SslContext}
     */
    public static SslContext forClient(Map<String, String> options) {
        final Iterable<String> ciphers = splitOrNull(options, OPT_CIPHERS);
        final String[] protocols = splitToArrayOrNull(options, OPT_PROTOCOLS);
        try {
            final KeyStoreFactory ksf = keyStoreFactoryFromOpts(options);
            final TrustManagerFactory trustManagerFactory = tmfFromOpts(ksf, options);
            final SslContextBuilder builder = SslContextBuilder.forClient()
                    .ciphers(ciphers)
                    .protocols(protocols)
                    .trustManager(trustManagerFactory);
            // we were asked to use client certificate for mutual authentication
            if (options.containsKey(SecurityConstants.OPT_CERT_ALIAS)) {
                final Object[] key = extractKeyMaterial(ksf, options);
                builder.keyManager((PrivateKey) key[0], (X509Certificate[]) key[1]);
            }
            return builder.build();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Unable to initialize client SSL context", e);
        }
    }

    /**
     * Create {@link SslContext} for server connection.
     *
     * @param options key-value map of URII options
     * @return {@link SslContext}
     */
    public static SslContext forServer(Map<String, String> options) {
        final Iterable<String> ciphers = splitOrNull(options, OPT_CIPHERS);
        final String[] protocols = splitToArrayOrNull(options, OPT_PROTOCOLS);
        try {
            final KeyStoreFactory ksf = keyStoreFactoryFromOpts(options);
            final ClientAuth clientAuth = clientAuthfromOpts(options);
            final TrustManagerFactory trustManagerFactory = tmfFromOpts(ksf, options);
            final Object[] serverCerts = extractKeyMaterial(ksf, options);
            final PrivateKey serverPrivateKey = (PrivateKey) serverCerts[0];
            final X509Certificate[] serverCertChain = (X509Certificate[]) serverCerts[1];
            return SslContextBuilder.forServer(serverPrivateKey, serverCertChain)
                    .clientAuth(clientAuth)
                    .protocols(protocols)
                    .ciphers(ciphers)
                    .trustManager(trustManagerFactory)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Unable to initialize server SSL context", e);
        }
    }

    private static ClientAuth clientAuthfromOpts(Map<String, String> options) {
        return ClientAuth.valueOf(options.getOrDefault(OPT_CLIENT_VERIFY, TLS_CLIENT_VERIFY_DEFAULT));
    }

    private static KeyStoreFactory keyStoreFactoryFromOpts(Map<String, String> options)
            throws GeneralSecurityException, IOException {
        final String type = options.getOrDefault(OPT_KEYSTORE_TYPE, KEYSTORE_TYPE_DEFAULT);
        if (KEYSTORE_TYPE_JKS.equalsIgnoreCase(type)) {
            return new JKSFactory(options);
        }
        if (KEYSTORE_TYPE_PKCS12.equalsIgnoreCase(type)) {
            return new PKCS12Factory(options);
        }
        throw new IllegalArgumentException("Unsupported KeyStore type " + type);
    }

    private static TrustManagerFactory tmfFromOpts(KeyStoreFactory ksf, Map<String, String> options)
            throws GeneralSecurityException {
        final String certPolicy = options.getOrDefault(OPT_CERT_POLICY, CERT_POLICY_DEFAULT);
        if (CERT_POLICY_IGNORE.equals(certPolicy)) {
            return InsecureTrustManagerFactory.INSTANCE;
        }
        return ChainLengthEnforcingTmf.create(ksf, options);
    }

    private static String findCertificateAlias(KeyStoreFactory ksf, Map<String, String> options)
            throws KeyStoreException {
        final String alias = options.get(SecurityConstants.OPT_CERT_ALIAS);
        if (alias == null) {
            final Enumeration<String> aliases = ksf.getKeyStore().aliases();
            while (aliases.hasMoreElements()) {
                final String candidate = aliases.nextElement();
                if (ksf.getKeyStore().isKeyEntry(candidate)) {
                    return candidate;
                }
            }
        }
        return alias;
    }

    private static Object[] extractKeyMaterial(KeyStoreFactory ksf, Map<String, String> options)
            throws GeneralSecurityException {
        final String alias = findCertificateAlias(ksf, options);
        Objects.requireNonNull(alias, "Certificate alias not specified and no private key found in KeyStore");
        final KeyStore ks = ksf.getKeyStore();
        Preconditions.checkState(ks.isKeyEntry(alias), "Alias '%s' is not private key", alias);
        final Entry entry = ks.getEntry(alias,
                new KeyStore.PasswordProtection(ksf.getKeyStorePassword().toCharArray()));
        final Certificate[] chain = ks.getCertificateChain(alias);
        final Object[] ret = new Object[2];
        ret[0] = ((PrivateKeyEntry) entry).getPrivateKey();
        ret[1] = new X509Certificate[chain.length];
        System.arraycopy(chain, 0, ret[1], 0, chain.length);
        return ret;
    }
}
