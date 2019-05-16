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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpsReqRepTest extends AbstractReqRepTest {
    private static final Logger LOG = LoggerFactory.getLogger(HttpsReqRepTest.class);

    @Test
    public void testSmallMessageSize() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        final int port = getFreeTcpPort();
        final String certFile = copyResource("/cert.p12");
        final String uri = new UriBuilder(getConnectUri(port))
                .add(Constants.OPT_CERT_FILE, certFile)
                .add(Constants.OPT_PRIVATE_KEY_PASSWORD, "123456")
                .add(Constants.OPT_CERT_TRUST, Constants.DEFAULT_CERT_POLICY)
                .build();

        testReqRep(uri, uri, "ABCD", "1234567890");
    }

    @Test
    public void testBigMessageSize() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        final int port = getFreeTcpPort();
        final String certFile = copyResource("/cert.p12");
        final String uri = new UriBuilder(getConnectUri(port))
                .add(Constants.OPT_CERT_FILE, certFile)
                .add(Constants.OPT_PRIVATE_KEY_PASSWORD, "123456")
                .add(Constants.OPT_CERT_TRUST, Constants.DEFAULT_CERT_POLICY)
                .build();

        testReqRep(uri, uri, "ABCD", "1234567890");
        testReqRep(uri, uri, Strings.repeat("X", 3000), Strings.repeat("Y", 2000));
    }

    @Test(expected = IllegalStateException.class)
    public void testFailureNoCertificate() {
        final int port = getFreeTcpPort();
        final String uri = new UriBuilder(getConnectUri(port))
                .add(Constants.OPT_CERT_FILE, UUID.randomUUID().toString())
                .add(Constants.OPT_PRIVATE_KEY_PASSWORD, UUID.randomUUID().toString())
                .build();
        factory.responder(uri, (peerContext, message) -> LOG.info("Received message {}", message));
    }

    @Override
    protected BusSessionFactory createFactory() {
        return new HttpsBusSessionFactory(group, group, group);
    }
}
