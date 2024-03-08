/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import com.google.common.collect.ImmutableClassToInstanceMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.binding.Rpc;

/**
 * Proxy context for remote RPC service which implements multiple yang models.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Oct 16, 2018
 */
public final class MultiRpcProxy extends AbstractRpcProxy {
    private final ImmutableClassToInstanceMap<Rpc<?, ?>> proxyMap;
    private final Set<SingleRpcProxy<?>> proxies;

    public MultiRpcProxy(final Set<SingleRpcProxy<?>> proxies) {
        this.proxies = proxies;
        proxyMap = ImmutableClassToInstanceMap.copyOf(proxies.stream()
            .collect(Collectors.toMap(SingleRpcProxy::getType, SingleRpcProxy::getProxy)));
    }

    /**
     * Get RPC proxy for particular {@link Rpc}.
     *
     * @param type subtype of {@link Rpc} to get proxy for
     * @return proxy for given RpcService subtype
     * @throws NoSuchElementException if there is no RPC proxy for specified type
     */
    public <T extends Rpc<?, ?>> @NonNull T getRpcService(final Class<T> type) {
        final var instance = proxyMap.getInstance(type);
        if (instance == null) {
            throw new NoSuchElementException("Service is not supported by this requester instance : " + type);
        }
        return instance;
    }

    @Override
    public boolean isConnectionReady() {
        return proxies.stream().allMatch(SingleRpcProxy::isConnectionReady);
    }

    @Override
    protected void removeRegistration() {
        proxies.stream().forEach(SingleRpcProxy::close);
    }
}
