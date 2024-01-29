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
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.Coffee;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.RemoveCoffeePot;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.RemoveCoffeePotInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.RemoveCoffeePotOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.RemoveCoffeePotOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public class TestRemoveCoffeePot implements RemoveCoffeePot {
    @Override
    public ListenableFuture<RpcResult<RemoveCoffeePotOutput>> invoke(RemoveCoffeePotInput input) {
        return RpcResultBuilder.success(new RemoveCoffeePotOutputBuilder()
            .setCupsBrewed(Uint32.valueOf(6))
            .setDrink(Coffee.VALUE)
            .build())
            .buildFuture();
    }
}
