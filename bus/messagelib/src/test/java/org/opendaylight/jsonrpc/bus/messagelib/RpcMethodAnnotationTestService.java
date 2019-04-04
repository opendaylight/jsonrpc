/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import org.opendaylight.jsonrpc.bus.api.RpcMethod;

/**
 * Purpose of this class is to exercise use of {@link RpcMethod}. TODO : Document me !!!
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Apr 4, 2019
 */
public interface RpcMethodAnnotationTestService {
    String simple();

    @RpcMethod("real-rpc-method")
    long notSoSimple();

    @RpcMethod("method.using.dots")
    int someDifferentMethod();
}
