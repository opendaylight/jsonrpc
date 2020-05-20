/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.tool.test;

import java.net.URISyntaxException;
import java.util.stream.LongStream;
import org.opendaylight.jsonrpc.bus.messagelib.ResponderSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.opendaylight.jsonrpc.test.FactorialInput;
import org.opendaylight.jsonrpc.test.FactorialOutput;
import org.opendaylight.jsonrpc.test.RemovecoffeepotOutput;
import org.opendaylight.jsonrpc.test.TestModelRpcService;

public class TestModelImpl implements TestModelRpcService {
    private final ResponderSession session;

    public TestModelImpl(TransportFactory transportFactory, String endpoint) throws URISyntaxException {
        session = transportFactory.endpointBuilder().responder().create(endpoint, this);
    }

    @Override
    public void simpleMethod() {
        // NOOP
    }

    @Override
    public void errorMethod() {
        throw new IllegalStateException("This method will fail when called");
    }

    @Override
    public FactorialOutput factorial(FactorialInput in) {
        return new FactorialOutput(LongStream.rangeClosed(1, in.getInNumber()).reduce(1, (long x, long y) -> x * y));
    }

    @Override
    public RemovecoffeepotOutput removecoffeepot() {
        return new RemovecoffeepotOutput(5L, "coffee");
    }

    @Override
    public void close() {
        Util.closeAndLogOnError(session);
    }

    @Override
    public String toString() {
        return "TestModelImpl [session=" + session + "]";
    }
}
