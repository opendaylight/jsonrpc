/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.security.api;

import java.security.KeyStore;

/**
 * Factory to obtain {@link KeyStore} instances.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 17, 2019
 */
public interface KeyStoreFactory {
    /**
     * Get {@link KeyStore} containing trust anchors.
     *
     * @return {@link KeyStore} containing trust anchors
     */
    KeyStore getTrustStore();

    /**
     * Get {@link KeyStore} containing client/server certificate + private key.
     *
     * @return {@link KeyStore} containing client/server certificate + private key
     */
    KeyStore getKeyStore();

    /**
     * Get password to unlock {@link KeyStore} returned by call to {@link #getKeyStore()}.
     *
     * @return password to unlock {@link KeyStore} returned by call to {@link #getKeyStore()}.
     */
    String getKeyStorePassword();
}
