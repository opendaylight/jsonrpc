/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import org.opendaylight.jsonrpc.model.JsonRpcTransactionFacade;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy that correctly propagates cause so that meaningful response can be generated by RESTConf.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 26, 2019
 */
public final class TransactionProxy implements InvocationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionProxy.class);
    private static final Set<String> READ_METHODS = ImmutableSet.of("read", "exists");
    private final JsonRpcTransactionFacade delegate;

    private TransactionProxy(JsonRpcTransactionFacade delegate) {
        this.delegate = delegate;
    }

    /**
     * Create new proxy instance using given delegate.
     *
     * @param delegate target instance to whom calls are intercepted
     * @return new proxy instance
     */
    public static JsonRpcTransactionFacade create(JsonRpcTransactionFacade delegate) {
        return (JsonRpcTransactionFacade) Proxy.newProxyInstance(JsonRpcTransactionFacade.class.getClassLoader(),
                new Class<?>[] { JsonRpcTransactionFacade.class }, new TransactionProxy(delegate));
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (Exception e) {
            final Throwable rootCause = Throwables.getRootCause(e);
            //only debug level, upper layer (restconf) already log same stacktrace on WARN level
            LOG.debug("Caught exception while invoking method '{}', propagating it to caller", method.getName(),
                    rootCause);
            if (READ_METHODS.contains(method.getName())) {
                return FluentFutures.immediateFailedFluentFuture(
                        new ReadFailedException("Invocation of method '" + method.getName() + "' failed", rootCause));
            } else if ("commit".equals(method.getName())) {
                return FluentFutures.immediateFailedFluentFuture(
                        new TransactionCommitFailedException("Commit of transaction failed", rootCause));
            } else {
                //this will be shown as HTTP500 in restconf, but there is nothing more we can do about it
                throw new IllegalStateException("Operation '" + method.getName() + "' failed", rootCause);
            }
        }
    }
}