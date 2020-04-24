/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.test;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.opendaylight.yangtools.yang.common.RpcResultBuilder.success;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.Coffee;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.ErrorMethodInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.ErrorMethodOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.FactorialInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.FactorialOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.FactorialOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.GetAllNumbersInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.GetAllNumbersOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.GetAnyXmlInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.GetAnyXmlOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.MethodWithAnyxmlInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.MethodWithAnyxmlOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.MultiplyListInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.MultiplyListOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.MultiplyListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.MultiplyLlInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.MultiplyLlOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.RemoveCoffeePotInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.RemoveCoffeePotOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.RemoveCoffeePotOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.SimpleMethodInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.SimpleMethodOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.TestModelService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.numbers.list.Numbers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.numbers.list.NumbersBuilder;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public class TestModelServiceImpl implements TestModelService {

    @Override
    public ListenableFuture<RpcResult<MultiplyListOutput>> multiplyList(MultiplyListInput input) {
        final List<Numbers> ret = new ArrayList<>();
        short multiplier = input.getMultiplier();
        for (Numbers i : input.getNumbers()) {
            ret.add(new NumbersBuilder().setNum(multiplier * i.getNum()).build());
        }
        return immediateFuture(success(new MultiplyListOutputBuilder().setNumbers(ret).build()).build());
    }

    @Override
    public ListenableFuture<RpcResult<FactorialOutput>> factorial(FactorialInput input) {
        int ret = 2;
        for (int i = 3; i <= input.getInNumber().intValue(); i++) {
            ret *= i;
        }
        return immediateFuture(success(new FactorialOutputBuilder().setOutNumber(Uint32.valueOf(ret)).build()).build());
    }

    @Override
    public ListenableFuture<RpcResult<GetAllNumbersOutput>> getAllNumbers(GetAllNumbersInput input) {
        // NOOP
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetAnyXmlOutput>> getAnyXml(GetAnyXmlInput input) {
        // NOOP
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<MultiplyLlOutput>> multiplyLl(MultiplyLlInput input) {
        // NOOP
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<RemoveCoffeePotOutput>> removeCoffeePot(RemoveCoffeePotInput input) {
        return immediateFuture(success(
                new RemoveCoffeePotOutputBuilder().setCupsBrewed(Uint32.valueOf(6)).setDrink(Coffee.class).build())
                        .build());
    }

    @Override
    public ListenableFuture<RpcResult<ErrorMethodOutput>> errorMethod(ErrorMethodInput input) {
        return immediateFuture(RpcResultBuilder.<ErrorMethodOutput>failed().withError(ErrorType.RPC, "Ha!").build());
    }

    @Override
    public ListenableFuture<RpcResult<MethodWithAnyxmlOutput>> methodWithAnyxml(MethodWithAnyxmlInput input) {
        // NOOP
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<SimpleMethodOutput>> simpleMethod(SimpleMethodInput input) {
        return immediateFuture(RpcResultBuilder.<SimpleMethodOutput>success().build());
    }
}
