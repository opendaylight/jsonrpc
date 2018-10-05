/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import com.google.common.base.Strings;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.api.Requester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpReqRepTest extends AbstractReqRepTest {
    private static final Logger LOG = LoggerFactory.getLogger(HttpReqRepTest.class);

    @Test
    public void testSmallMessageSize() throws InterruptedException, ExecutionException, TimeoutException {
        final int port = getFreeTcpPort();
        testReqRep(getConnectUri(port), getBindUri(port), "ABCD", "1234567890");
    }

    @Test
    public void testBigMessageSize() throws InterruptedException, ExecutionException, TimeoutException {
        final int port = getFreeTcpPort();
        testReqRep(getConnectUri(port), getBindUri(port), Strings.repeat("X", 3000), Strings.repeat("Y", 2000));
    }

    @Test(expected = TimeoutException.class)
    public void testConnectionFailed() throws InterruptedException, ExecutionException, TimeoutException {
        final int port = getFreeTcpPort();
        final Requester requester = factory.requester(getConnectUri(port),
            (peerContext, message) -> LOG.info("Received response {}", message));
        requester.send("", 0, null).get(1, TimeUnit.SECONDS);
    }

    @Override
    protected BusSessionFactory createFactory() {
        return new HttpBusSessionFactory(config);
    }
}
