/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import java.util.Arrays;

public class RpcComplicatedParams {
    private String str1;
    private int int1;
    private innerComplicatedParam inner;

    private class innerComplicatedParam {
        String[] strArr;
        int[] intArr;

        public innerComplicatedParam(String[] strArr, int[] intArr) {
            this.strArr = strArr;
            this.intArr = intArr;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            innerComplicatedParam other = (innerComplicatedParam) obj;
            if (!Arrays.equals(intArr, other.intArr))
                return false;
            if (!Arrays.equals(strArr, other.strArr))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "innerComplicatedParam [strArr=" + Arrays.toString(strArr) + ", intArr=" + Arrays.toString(intArr)
                    + "]";
        }
    }

    public RpcComplicatedParams(String str1, int int1, String[] strArr, int[] intArr) {
        this.str1 = str1;
        this.int1 = int1;
        this.inner = new innerComplicatedParam(strArr, intArr);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RpcComplicatedParams other = (RpcComplicatedParams) obj;
        if (inner == null) {
            if (other.inner != null)
                return false;
        } else if (!inner.equals(other.inner))
            return false;
        if (int1 != other.int1)
            return false;
        if (str1 == null) {
            if (other.str1 != null)
                return false;
        } else if (!str1.equals(other.str1))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "RpcComplicatedParams [str1=" + str1 + ", int1=" + int1 + ", inner=" + inner + "]";
    }
}
