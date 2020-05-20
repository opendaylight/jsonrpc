/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.impl;

import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.mdsal.dom.api.DOMService;

public class ProxiedDOMService<T extends DOMService> extends AbstractInvocationHandler {
    private final AtomicReference<T> ref = new AtomicReference<>();
    private final AtomicBoolean disabled = new AtomicBoolean(true);
    private final T proxy;

    public ProxiedDOMService(Class<T> type) {
        proxy = Reflection.newProxy(type, this);
    }

    public void reset(T newRef) {
        ref.set(newRef);
    }

    public void disable(boolean disabled) {
        this.disabled.set(disabled);
    }

    public T getProxy() {
        return proxy;
    }

    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
        if (disabled.get()) {
            throw new IllegalStateException("DOMService is not avialable at this time");
        }
        return method.invoke(ref.get(), args);
    }
}
