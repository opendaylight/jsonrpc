/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.test;

import org.opendaylight.jsonrpc.bus.api.RpcMethod;

public interface TestModelRpcService extends AutoCloseable {

    @RpcMethod("simple-method")
    void simpleMethod();

    @RpcMethod("error-method")
    void errorMethod();

    @RpcMethod("factorial")
    FactorialOutput factorial(FactorialInput input);

    @RpcMethod("factorial")
    default FactorialOutput factorial(java.lang.Integer inNumber) {
        return factorial(new FactorialInput(inNumber));
    }

    @RpcMethod("removeCoffeePot")
    RemovecoffeepotOutput removecoffeepot();

    @Override
    default void close() {
        // NOOP
    }
}
