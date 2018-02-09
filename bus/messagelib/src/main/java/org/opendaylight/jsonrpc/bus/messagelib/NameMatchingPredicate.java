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
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcBaseRequestMessage;

/**
 * Match method based on name: For example, method name in JSON-RPC message
 * is 'method-abc'. Match will occur when java method:
 * <ol>
 * <li>is named 'methodAbc'</li>
 * <li>is named 'method_abc'</li>
 * </ol>
 * the number of method arguments is not a factor in the match
 */
public class NameMatchingPredicate implements Predicate<Method> {
    protected final JsonRpcBaseRequestMessage msg;

    public NameMatchingPredicate(JsonRpcBaseRequestMessage msg) {
        this.msg = msg;
    }

    @Override
    public boolean test(Method method) {
        boolean nameMatched = false;
        nameMatched |= method.getName().equalsIgnoreCase(toUnderscoreName(msg.getMethod()));
        nameMatched |= method.getName().equalsIgnoreCase(toCamelCaseName(msg.getMethod()));
        return nameMatched;
    }

    /**
     * Convert raw method name to name with underscores. <br />
     * <p>
     * Examples:
     * </p>
     * <ul>
     * <li>method-abc =&gt; method_abc</li>
     * <li>method_def =&gt; method_def</li>
     * <li>method123 => method123</li>
     * </ul>
     */
    private String toUnderscoreName(String name) {
        return name.replaceAll("-", "_").replaceAll("\\.", "_");
    }

    /**
     * Convert raw method name to one with camel case.
     */
    private String toCamelCaseName(String name) {
        final String ret = Arrays.asList(name.split("_|-|\\.")).stream().map(n -> {
            if (n.length() > 0) {
                return n.substring(0, 1).toUpperCase() + n.substring(1);
            } else {
                return n.toUpperCase();
            }
        }).collect(Collectors.joining());
        return Character.toLowerCase(ret.charAt(0)) + ret.substring(1);
    }
}