/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.security.api;

import static java.lang.Integer.parseInt;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.OPT_CLIENT_CHAIN_LENGTH;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.OPT_SERVER_CHAIN_LENGTH;
import static org.opendaylight.jsonrpc.security.api.SecurityConstants.TLS_VERIFY_DEPTH_DEFAULT;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

/**
 * {@link TrustManagerFactory} that can enforce CA chain length.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 17, 2019
 */
class ChainLengthEnforcingTmf extends TrustManagerFactory {
    /**
     * Create new instance of {@link TrustManagerFactory} from given URI options.
     *
     * @param options URI options
     * @return new instance of {@link TrustManagerFactory}
     * @throws GeneralSecurityException if {@link TrustManagerFactory} can't be created
     */
    static TrustManagerFactory create(KeyStoreFactory ksf, Map<String, String> options)
            throws GeneralSecurityException {
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        X509TrustManager x509tm = null;
        tmf.init(ksf.getTrustStore());
        for (TrustManager provider : tmf.getTrustManagers()) {
            if (provider instanceof X509TrustManager) {
                x509tm = (X509TrustManager) provider;
                break;
            }
        }
        Objects.requireNonNull(x509tm, "Can't find X509TrustManager");
        int clientLength = parseInt(options.getOrDefault(OPT_CLIENT_CHAIN_LENGTH, TLS_VERIFY_DEPTH_DEFAULT));
        int serverLength = parseInt(options.getOrDefault(OPT_SERVER_CHAIN_LENGTH, TLS_VERIFY_DEPTH_DEFAULT));
        return new ChainLengthEnforcingTmf(clientLength, serverLength, x509tm);
    }

    ChainLengthEnforcingTmf(int clientChainLength, int serverChainLength, X509TrustManager x509tm) {
        super(new TrustManagerFactorySpi() {

            @Override
            protected void engineInit(ManagerFactoryParameters spec) throws InvalidAlgorithmParameterException {
                // NOOP
            }

            @Override
            protected void engineInit(KeyStore ks) throws KeyStoreException {
                // NOOP
            }

            @Override
            protected TrustManager[] engineGetTrustManagers() {
                return new TrustManager[] {
                    new ChainLengthEnforcingTrustManager(x509tm, clientChainLength, serverChainLength) };
            }
        }, null, null);
    }
}
