/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import com.google.common.base.Strings;

public class MockHandler implements AutoCloseable{
    public void method1() {

    }

    public String method_2(int a, String str) {
        return Strings.repeat(str, a);
    }

    public double methodWithCamelCase(int in) {
        return Math.pow(2, in);
    }

    public int similar_method_name(String a) {
        return a.length();
    }

    public int similarMethodName(String a) {
        return a.length() + 10;
    }

    @Override
    public void close() throws Exception {
        //no-op
    }

}
