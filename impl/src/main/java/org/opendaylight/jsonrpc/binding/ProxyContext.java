/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import java.util.function.Consumer;
import org.opendaylight.jsonrpc.bus.messagelib.BaseSession;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.RpcService;

/**
 * Context of proxy. Allows proper cleanup of created instance.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 21, 2018
 */
public class ProxyContext<T extends RpcService> implements AutoCloseable {
    private final ObjectRegistration<T> rpcRegistration;
    private final BaseSession session;
    private final T proxy;
    private final Consumer<T> cleanCallback;
    private final Class<T> type;

    ProxyContext(final Class<T> type, final ObjectRegistration<T> rpcRegistration, final BaseSession session,
            final T proxy, final Consumer<T> cleanCallback) {
        this.rpcRegistration = rpcRegistration;
        this.session = session;
        this.proxy = proxy;
        this.cleanCallback = cleanCallback;
        this.type = type;
    }

    public T getProxy() {
        return proxy;
    }

    @Override
    public void close() {
        cleanCallback.accept(proxy);
    }

    public Class<T> getType() {
        return type;
    }

    void closeInternal() {
        session.close();
        rpcRegistration.close();
    }
}
