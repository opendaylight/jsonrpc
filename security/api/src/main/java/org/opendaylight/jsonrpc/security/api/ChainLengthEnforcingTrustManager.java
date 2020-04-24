/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.security.api;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChainLengthEnforcingTrustManager implements X509TrustManager {
    private static final Logger LOG = LoggerFactory.getLogger(ChainLengthEnforcingTrustManager.class);
    private final int clientChainMaxLength;
    private final int serverChainMaxLength;
    private final X509TrustManager delegate;

    public ChainLengthEnforcingTrustManager(X509TrustManager delegate, int clientChainMaxLength,
            int serverChainMaxLength) {
        this.delegate = Objects.requireNonNull(delegate);
        this.clientChainMaxLength = clientChainMaxLength;
        this.serverChainMaxLength = serverChainMaxLength;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        checkChainLength(chain, serverChainMaxLength, "client");
        if (chain.length > serverChainMaxLength) {
            throw new CertificateException("Trust chain length exceeds configured value " + serverChainMaxLength);
        }
        delegate.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        checkChainLength(chain, clientChainMaxLength, "server");
        delegate.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return delegate.getAcceptedIssuers();
    }

    private void checkChainLength(X509Certificate[] chain, int configured, String type) throws CertificateException {
        LOG.debug("Checking chain length {} against configured value {} for {}", chain.length, configured, type);
        if (chain.length > configured) {
            throw new CertificateException(
                    "Trust chain length exceeds configured value for " + type + " : " + configured);
        }
    }
}
