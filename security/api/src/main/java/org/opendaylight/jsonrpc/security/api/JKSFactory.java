/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.security.api;

import static org.opendaylight.jsonrpc.security.api.SecurityConstants.KEYSTORE_TYPE_JKS;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.OPT_KEYSTORE_FILE;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.OPT_KEYSTORE_PASSWORD;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.OPT_TRUSTSTORE_FILE;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.OPT_TRUSTSTORE_PASSWORD;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link KeyStoreFactory} for JKS {@link KeyStore}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 17, 2019
 */
public class JKSFactory extends AbstractKeyStoreFactory {
    private static final Logger LOG = LoggerFactory.getLogger(JKSFactory.class);
    private static final String DEFAULT_TRUSTSTORE_LOCATION;
    private static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";

    static {
        DEFAULT_TRUSTSTORE_LOCATION = "$JAVA_HOME/lib/security/cacerts".replace("$JAVA_HOME",
                System.getProperty("java.home"));
    }

    /**
     * Create new {@link JKSFactory} using given options.
     *
     * @param options key-value pairs of options
     * @throws GeneralSecurityException if {@link KeyStore} can't be created.
     * @throws IOException if {@link KeyStore} can't be loaded
     */
    @SuppressFBWarnings("OS_OPEN_STREAM")
    public JKSFactory(final Map<String, String> options) throws GeneralSecurityException, IOException {
        Objects.requireNonNull(options);
        trustStore = KeyStore.getInstance(KEYSTORE_TYPE_JKS);
        final String trustStoreFile = options.getOrDefault(OPT_TRUSTSTORE_FILE, DEFAULT_TRUSTSTORE_LOCATION);
        final String trustStorePassword = options.getOrDefault(OPT_TRUSTSTORE_PASSWORD, DEFAULT_KEYSTORE_PASSWORD);
        LOG.debug("Loading trust KeyStore from {}", trustStoreFile);
        trustStore.load(Files.newInputStream(Path.of(trustStoreFile)), trustStorePassword.toCharArray());
        final String storeFile = options.getOrDefault(OPT_KEYSTORE_FILE, trustStoreFile);
        keyStorePassword = options.getOrDefault(OPT_KEYSTORE_PASSWORD, trustStorePassword);
        LOG.debug("Loading KeyStore from {}", storeFile);
        keyStore = KeyStore.getInstance(KEYSTORE_TYPE_JKS);
        keyStore.load(Files.newInputStream(Path.of(storeFile)), keyStorePassword.toCharArray());
    }
}
