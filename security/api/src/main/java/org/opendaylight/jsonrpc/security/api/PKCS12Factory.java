/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.security.api;

import static org.opendaylight.jsonrpc.security.api.SecurityConstants.KEYSTORE_TYPE_PKCS12;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.OPT_KEYSTORE_FILE;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.OPT_KEYSTORE_PASSWORD;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link KeyStoreFactory} backed by PKCS12 {@link KeyStore}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 17, 2019
 */
public class PKCS12Factory extends AbstractKeyStoreFactory {
    private static final Logger LOG = LoggerFactory.getLogger(PKCS12Factory.class);

    @SuppressFBWarnings("OS_OPEN_STREAM")
    public PKCS12Factory(Map<String, String> options) throws GeneralSecurityException, IOException {
        final String trustStoreFile = Objects.requireNonNull(options.get(OPT_KEYSTORE_FILE));
        keyStorePassword = Objects.requireNonNull(options.get(OPT_KEYSTORE_PASSWORD));
        LOG.debug("Loading KeyStore from {}", trustStoreFile);
        trustStore = KeyStore.getInstance(KEYSTORE_TYPE_PKCS12);
        trustStore.load(Files.newInputStream(Paths.get(trustStoreFile)), keyStorePassword.toCharArray());
        keyStore = trustStore;
    }
}
