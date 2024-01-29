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
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.SimpleMethod;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.SimpleMethodInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.SimpleMethodOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class TestSimpleMethod implements SimpleMethod {
    @Override
    public ListenableFuture<RpcResult<SimpleMethodOutput>> invoke(SimpleMethodInput input) {
        return RpcResultBuilder.<SimpleMethodOutput>success().buildFuture();
    }
}
