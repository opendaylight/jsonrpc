/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import java.util.function.Consumer;
import org.opendaylight.jsonrpc.bus.messagelib.RequesterSession;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Rpc;

/**
 * Context of proxy. Allows proper cleanup of created instance.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 21, 2018
 */
public final class SingleRpcProxy<T extends Rpc<?, ?>> extends AbstractRpcProxy {
    private final Registration rpcRegistration;
    private final RequesterSession session;
    private final T proxy;
    private final Consumer<T> cleanCallback;
    private final Class<T> type;

    SingleRpcProxy(final Class<T> type, final Registration rpcRegistration, final RequesterSession session,
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
    public boolean isConnectionReady() {
        return session.isConnectionReady();
    }

    @Override
    protected void removeRegistration() {
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
