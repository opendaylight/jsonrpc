/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import java.lang.reflect.Method;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcBaseRequestMessage;

/**
 * A matcher that not only uses the method name but also the number of
 * arguments.
 */
public class StrictMatchingPredicate extends NameMatchingPredicate {

    public StrictMatchingPredicate(JsonRpcBaseRequestMessage msg) {
        super(msg);
    }

    private boolean nameMatches(Method method) {
        return super.test(method);
    }

    private boolean parameterCountMatches(Method method) {
        return Util.getParametersCount(msg) == method.getParameterTypes().length;
    }

    @Override
    public boolean test(Method method) {
        return nameMatches(method) && parameterCountMatches(method);
    }
}
