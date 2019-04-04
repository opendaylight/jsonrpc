/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Test;

public class RpcMethodAnnotationTest {
    @Test
    public void test() {
        final Map<String, Method> methods = Arrays.asList(RpcMethodAnnotationTestService.class.getDeclaredMethods())
                .stream()
                .collect(Collectors.toMap(Method::getName, Function.identity()));
        assertEquals("simple", ProxyServiceImpl.getMethodName(methods.get("simple")));
        assertEquals("real-rpc-method", ProxyServiceImpl.getMethodName(methods.get("notSoSimple")));
        assertEquals("method.using.dots", ProxyServiceImpl.getMethodName(methods.get("someDifferentMethod")));
    }
}
