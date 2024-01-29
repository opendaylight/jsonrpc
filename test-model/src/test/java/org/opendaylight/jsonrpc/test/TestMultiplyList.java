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
import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.base.rev201014.numbers.list.NumbersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.MultiplyList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.MultiplyListInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.MultiplyListOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.MultiplyListOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class TestMultiplyList implements MultiplyList {
    @Override
    public ListenableFuture<RpcResult<MultiplyListOutput>> invoke(MultiplyListInput input) {
        final short multiplier = input.getMultiplier();
        final var map = input.nonnullNumbers().entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(),
                new NumbersBuilder().setNum(e.getValue().getNum() * multiplier).build()))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        return RpcResultBuilder.success(new MultiplyListOutputBuilder().setNumbers(map).build()).buildFuture();
    }
}
