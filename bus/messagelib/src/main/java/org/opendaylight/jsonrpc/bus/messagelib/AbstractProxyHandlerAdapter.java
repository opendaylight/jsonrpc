/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcBaseRequestMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for proxy handlers.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Apr 2, 2018
 */
abstract class AbstractProxyHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractProxyHandlerAdapter.class);
    protected final Object handler;
    private final List<Method> methods;

    AbstractProxyHandlerAdapter(final boolean skipOutputMethods, final Object handler) {
        this.handler = Objects.requireNonNull(handler);
        // cache this at construction
        methods = filterOutputMethods(handler.getClass().getMethods(), skipOutputMethods);
    }

    /*
     * Notifications don't want output
     */
    private List<Method> filterOutputMethods(final Method[] decaredMethods, final boolean skipOutputMethods) {
        return Arrays.stream(decaredMethods).filter(method -> {
            if (skipOutputMethods) {
                // allow only methods which has return type 'void'
                return method.getReturnType().equals(void.class);
            } else {
                return true;
            }
        }).collect(Collectors.toList());
    }

    protected List<Method> findMethodStrict(final JsonRpcBaseRequestMessage msg) {
        return methods.stream()
                .filter(new StrictMatchingPredicate(msg))
                .sorted(Util.nameAndArgsSorter())
                .sorted(Util.payloadAwareSorter(msg.getParams()))
                .collect(Collectors.toList());
    }

    protected List<Method> findMethodLenient(final JsonRpcBaseRequestMessage msg) {
        return methods.stream()
                .filter(new NameMatchingPredicate(msg))
                .sorted(Util.nameAndArgsSorter())
                .collect(Collectors.toList());
    }

    @SuppressFBWarnings("SLF4J_FORMAT_SHOULD_BE_CONST")
    @SuppressWarnings({ "squid:S1166", "squid:S00112", "checkstyle:IllegalThrows" })
    protected Object invokeHandler(JsonRpcBaseRequestMessage message) throws Exception {
        final List<MethodCandidate> candidates = new ArrayList<>();
        List<Method> opt = findMethodStrict(message);
        if (!opt.isEmpty()) {
            // We have a method with the incoming method name and have
            // managed to parse parameters as per that method's signature.
            // This could be fooled if the parsing of incoming parameters
            // somehow works for a different set of parameters. (i.e.
            // objects with members similar to another might be parsed
            // as one or other
            for (final Method m : opt) {
                final MethodCandidate mc = new MethodCandidate(handler, m);
                LOG.debug("Attempting method candidate {}", mc);
                candidates.add(mc);
                // invoke inhibits all exceptions
                mc.invoke(message);
                if (mc.isSuccess()) {
                    return mc.result();
                }
            }
            throw findClosestFailure(candidates);
        } else {
            // At this point it could be wrong number of arguments.
            opt = findMethodLenient(message);
            if (!opt.isEmpty()) {
                String msg = String.format("Found method but wrong number of arguments: %s", message.getMethod());
                LOG.debug(msg);
                throw new IllegalArgumentException(msg);
            }
        }
        throw new NoSuchMethodException(String.format("Method not found : %s", message.getMethod()));
    }

    /**
     * In case of multiple method candidates tried, there might be different
     * reasons why invocation failed.It is safe to call underlying
     * {@link Optional#get()} without checking {@link Optional#isPresent()},
     * because it is guaranteed that at least one exception is found when this
     * code runs.
     *
     * @param candidates list of {@link MethodCandidate} tried.
     * @see MethodCandidate#getFailure()
     */
    @SuppressWarnings("squid:S3655")
    private Exception findClosestFailure(List<MethodCandidate> candidates) {
        return Stream
                .concat(candidates.stream().filter(c -> c.getPostInvokeFailure() != null),
                        candidates.stream().filter(c -> c.getPreInvokeFailure() != null))
                .findFirst()
                .orElseThrow()
                .getFailure();
    }

    /*
     * Extract the error message from an exception. First check if there is a
     * nested exception. If no error message is available, then use a fixed
     * string.
     */
    protected String getErrorMessage(Exception error) {
        String errorMessage;
        Throwable inner = error.getCause();
        if (inner != null) {
            errorMessage = inner.getMessage();
        } else {
            errorMessage = error.getMessage();
        }
        // Ensure some error string is sent
        if (errorMessage == null) {
            errorMessage = JsonRpcErrorObject.JSONRPC_ERROR_MESSAGE_INTERNAL;
        }
        return errorMessage;
    }
}
