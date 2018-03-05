/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import java.lang.reflect.Method;

import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcBaseRequestMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class encompasses method candidate invocation result and failure. There
 * are 2 phases where exception can be caught:
 * <ol>
 * <li>pre-invocation - argument parsing</li>
 * <li>post-invocation - after method has been invoked.</li>
 * </ol>
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 3, 2018
 */
public class MethodCandidate {
    private static final Logger LOG = LoggerFactory.getLogger(MethodCandidate.class);
    private final Method method;
    private final Object handler;
    private Object result;
    private Exception preInvokeFailure;
    private Exception postInvokeFailure;

    public MethodCandidate(final Object handler, final Method method) {
        this.method = method;
        this.handler = handler;
    }

    /*
     * Create array of values to be used as method arguments. This method is
     * assuming that number of parameters is already matching number of
     * arguments
     */
    private Object[] getArgumentsForMethod(JsonRpcBaseRequestMessage message) throws JsonRpcException {
        final Object[] args = new Object[Util.getParametersCount(message)];
        final Class<?>[] argsTypes = method.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            args[i] = message.getParamsAtIndexAsObject(i, argsTypes[i]);
        }
        return args;
    }

    /**
     * Invoke method and collect result. In case of failure, exception is caught
     * and saved for later examination.
     *
     * @param message JSON-RPC request to pass to method
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void invoke(final JsonRpcBaseRequestMessage message) {
        Object[] args;
        try {
            args = getArgumentsForMethod(message);
        } catch (Exception e) {
            LOG.debug("Unable to extract arguments for method", e);
            preInvokeFailure = e;
            return;
        }
        try {
            result = method.invoke(handler, args);
        } catch (Exception e) {
            postInvokeFailure = e;
            LOG.debug("Invocation of method candidate '{}' failed", e);
        }
    }

    /**
     * Check if method invocation succeeded.
     */
    public boolean isSuccess() {
        return preInvokeFailure == null && postInvokeFailure == null;
    }

    /**
     * Result of invocation is guaranteed to be available if
     * {@link #isSuccess()} return true.
     */
    public Object result() {
        return result;
    }

    /**
     * Failure of post-invocation phase.
     */
    public Exception getPostInvokeFailure() {
        return postInvokeFailure;
    }

    /**
     * Failure of pre-invocation phase.
     */
    public Exception getPreInvokeFailure() {
        return preInvokeFailure;
    }

    /**
     * Get failure of this candidate invocation. Post-invocation takes
     * precedence over pre-invocation phase.
     *
     */
    public Exception getFailure() {
        if (postInvokeFailure != null) {
            return postInvokeFailure;
        }
        if (preInvokeFailure != null) {
            return preInvokeFailure;
        }
        return null;
    }

    @Override
    public String toString() {
        return "MethodCandidate [method=" + method + "]";
    }
}
