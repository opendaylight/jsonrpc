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
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;

public class WsReqRepTest extends AbstractReqRepTest {
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

    @Override
    protected BusSessionFactory createFactory() {
        return new WsBusSessionFactory(config);
    }
}
