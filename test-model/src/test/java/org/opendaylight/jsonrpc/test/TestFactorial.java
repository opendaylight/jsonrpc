/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 * Copyright (c) 2024 PANTHEON.tech, s.r.o.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.test;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.Factorial;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.FactorialInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.FactorialOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.FactorialOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public class TestFactorial implements Factorial {
    @Override
    public ListenableFuture<RpcResult<FactorialOutput>> invoke(FactorialInput input) {
        int ret = 2;
        for (int i = 3; i <= input.getInNumber().intValue(); i++) {
            ret *= i;
        }
        return RpcResultBuilder.success(new FactorialOutputBuilder().setOutNumber(Uint32.valueOf(ret)).build())
            .buildFuture();
    }
}
