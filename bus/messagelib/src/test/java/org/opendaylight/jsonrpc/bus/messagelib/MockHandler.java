/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import com.google.common.base.Strings;

public class MockHandler implements AutoCloseable {
    private int counter = 0;

    public int getCount() {
        return counter;
    }

    public void method1() {
    }

    public String method_2(int count, String str) {
        return Strings.repeat(str, count);
    }

    public double methodWithCamelCase(int in) {
        return Math.pow(2, in);
    }

    public int similar_method_name(String str) {
        return str.length();
    }

    public int similarMethodName(String str) {
        return str.length() + 10;
    }

    public int match_test(String arg1, int arg2, String arg3) {
        counter++;
        return 1;
    }

    public int match_test(int arg1, String arg2, int arg3) {
        counter++;
        return 1;
    }

    public String match_test(float arg1, String arg2, String arg3) {
        throw new RuntimeException("Should fail");
    }

    public int match_test2(int arg1, int arg2) {
        return 1;
    }

    public int match_test2(String arg1, String arg2) {
        throw new IllegalStateException("Should fail");
    }

    @Override
    public void close() throws Exception {
        // no-op
    }
}