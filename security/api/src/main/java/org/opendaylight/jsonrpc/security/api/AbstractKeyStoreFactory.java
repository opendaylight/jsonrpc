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
 * Abstract class for {@link KeyStoreFactory} implementations.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 17, 2019
 */
abstract class AbstractKeyStoreFactory implements KeyStoreFactory {
    protected KeyStore trustStore;
    protected KeyStore keyStore;
    protected String keyStorePassword;

    @Override
    public KeyStore getTrustStore() {
        return trustStore;
    }

    @Override
    public KeyStore getKeyStore() {
        return keyStore;
    }

    @Override
    public String getKeyStorePassword() {
        return keyStorePassword;
    }
}
