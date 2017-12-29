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
    private final String str1;
    private final int int1;
    private final InnerComplicatedParam inner;

    private class InnerComplicatedParam {
        String[] strArr;
        int[] intArr;

        InnerComplicatedParam(String[] strArr, int[] intArr) {
            this.strArr = strArr;
            this.intArr = intArr;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + Arrays.hashCode(intArr);
            result = prime * result + Arrays.hashCode(strArr);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            InnerComplicatedParam other = (InnerComplicatedParam) obj;
            if (!Arrays.equals(intArr, other.intArr)) {
                return false;
            }
            if (!Arrays.equals(strArr, other.strArr)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "innerComplicatedParam [strArr=" + Arrays.toString(strArr) + ", intArr=" + Arrays.toString(intArr)
                    + "]";
        }

        private RpcComplicatedParams getOuterType() {
            return RpcComplicatedParams.this;
        }
    }

    public RpcComplicatedParams(String str1, int int1, String[] strArr, int[] intArr) {
        this.str1 = str1;
        this.int1 = int1;
        this.inner = new InnerComplicatedParam(strArr, intArr);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (inner == null ? 0 : inner.hashCode());
        result = prime * result + int1;
        result = prime * result + (str1 == null ? 0 : str1.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RpcComplicatedParams other = (RpcComplicatedParams) obj;
        if (inner == null) {
            if (other.inner != null) {
                return false;
            }
        } else if (!inner.equals(other.inner)) {
            return false;
        }
        if (int1 != other.int1) {
            return false;
        }
        if (str1 == null) {
            if (other.str1 != null) {
                return false;
            }
        } else if (!str1.equals(other.str1)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "RpcComplicatedParams [str1=" + str1 + ", int1=" + int1 + ", inner=" + inner + "]";
    }
}
