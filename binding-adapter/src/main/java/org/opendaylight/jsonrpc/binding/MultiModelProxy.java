/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import com.google.common.collect.ImmutableClassToInstanceMap;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.yangtools.yang.binding.Rpc;

/**
 * Proxy context for remote RPC service which implements multiple yang models.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Oct 16, 2018
 */
public class MultiModelProxy implements AutoCloseable {
    private final ImmutableClassToInstanceMap<Rpc<?, ?>> proxyMap;
    private final Set<ProxyContext<?>> proxies;

    public MultiModelProxy(Set<ProxyContext<?>> proxies) {
        this.proxies = proxies;
        proxyMap = ImmutableClassToInstanceMap.copyOf(proxies.stream()
            .collect(Collectors.toMap(ProxyContext::getType, ProxyContext::getProxy)));
    }

    @Override
    public void close() {
        proxies.stream().forEach(ProxyContext::close);
    }

    /**
     * Get RPC proxy for particular {@link RpcService}.
     *
     * @param type subtype of {@link RpcService} to get proxy for
     * @return proxy for given RpcService subtype
     */
    public <T extends Rpc<?, ?>> T getRpcService(Class<T> type) {
        return Objects.requireNonNull(proxyMap.getInstance(type),
            "Service is not supported by this requester instance : " + type);
    }
}
