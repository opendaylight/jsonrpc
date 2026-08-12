/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

public class MockHandler implements AutoCloseable {
    private int counter = 0;

    public int getCount() {
        return counter;
    }

    public void method1() {
    }

    public String method_2(final int count, final String str) {
        return str.repeat(count);
    }

    public double methodWithCamelCase(final int in) {
        return Math.pow(2, in);
    }

    public int similar_method_name(final String str) {
        return str.length();
    }

    public int similarMethodName(final String str) {
        return str.length() + 10;
    }

    public int match_test(final String arg1, final int arg2, final String arg3) {
        counter++;
        return 1;
    }

    public int match_test(final int arg1, final String arg2, final int arg3) {
        counter++;
        return 1;
    }

    public String match_test(final float arg1, final String arg2, final String arg3) {
        throw new RuntimeException("Should fail");
    }

    public int match_test2(final int arg1, final int arg2) {
        return 1;
    }

    public int match_test2(final String arg1, final String arg2) {
        throw new IllegalStateException("Should fail");
    }

    @Override
    public void close() throws Exception {
        // no-op
    }
}