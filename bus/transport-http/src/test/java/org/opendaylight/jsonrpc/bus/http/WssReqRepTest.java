/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import com.google.common.base.Strings;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.api.UriBuilder;
import org.opendaylight.jsonrpc.security.api.SecurityConstants;
import org.opendaylight.jsonrpc.security.noop.NoopSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WssReqRepTest extends AbstractReqRepTest {
    private static final Logger LOG = LoggerFactory.getLogger(WssReqRepTest.class);

    @Test(timeout = 15_000)
    public void testSmallMessageSize() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        final int port = getFreeTcpPort();
        final String certFile = copyResource("/cert.p12");
        final String serverUri = new UriBuilder(getBindUri(port))
                .add(SecurityConstants.OPT_KEYSTORE_TYPE, SecurityConstants.KEYSTORE_TYPE_PKCS12)
                .add(SecurityConstants.OPT_KEYSTORE_FILE, certFile)
                .add(SecurityConstants.OPT_KEYSTORE_PASSWORD, "123456")
                .add(SecurityConstants.OPT_CERT_POLICY, SecurityConstants.CERT_POLICY_IGNORE)
                .build();
        final String clientUri = new UriBuilder(getConnectUri(port))
                .add(SecurityConstants.OPT_CERT_POLICY, SecurityConstants.CERT_POLICY_IGNORE)
                .build();
        testReqRep(clientUri, serverUri, "ABCD", "1234567890");
    }

    @Test(timeout = 15_000)
    public void testBigMessageSize() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        final int port = getFreeTcpPort();
        final String certFile = copyResource("/cert.p12");
        final String serverUri = new UriBuilder(getBindUri(port))
                .add(SecurityConstants.OPT_KEYSTORE_TYPE, SecurityConstants.KEYSTORE_TYPE_PKCS12)
                .add(SecurityConstants.OPT_KEYSTORE_FILE, certFile)
                .add(SecurityConstants.OPT_KEYSTORE_PASSWORD, "123456")
                .add(SecurityConstants.OPT_CERT_POLICY, SecurityConstants.CERT_POLICY_IGNORE)
                .build();
        final String clientUri = new UriBuilder(getConnectUri(port))
                .add(SecurityConstants.OPT_CERT_POLICY, SecurityConstants.CERT_POLICY_IGNORE)
                .build();

        testReqRep(clientUri, serverUri, "ABCD", "1234567890");
        testReqRep(clientUri, serverUri, Strings.repeat("X", 3000), Strings.repeat("Y", 2000));
    }

    @Test(timeout = 15_000, expected = IllegalStateException.class)
    public void testFailureNoCertificate() {
        final int port = getFreeTcpPort();
        final String uri = new UriBuilder(getBindUri(port))
                .add(SecurityConstants.OPT_KEYSTORE_TYPE, SecurityConstants.KEYSTORE_TYPE_PKCS12)
                .add(SecurityConstants.OPT_KEYSTORE_FILE, UUID.randomUUID().toString())
                .add(SecurityConstants.OPT_KEYSTORE_PASSWORD, UUID.randomUUID().toString())
                .build();
        factory.responder(uri, (peerContext, message) -> LOG.info("Received message {}", message));
    }

    @Override
    protected BusSessionFactory createFactory() {
        return new WssBusSessionFactory(config, NoopSecurityService.INSTANCE);
    }
}
