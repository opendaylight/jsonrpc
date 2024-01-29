/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import java.util.Optional;
import java.util.Set;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage.Builder;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.opendaylight.jsonrpc.bus.messagelib.RequestMessageHandler;

/**
 * {@link RequestMessageHandler} which dispatch incoming RPC request to correct implementation.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Oct 16, 2018
 */
public class MultiModelRequestDispatcher implements RequestMessageHandler {
    private final Set<InboundHandler<RpcService>> handlers;

    MultiModelRequestDispatcher(Set<InboundHandler<RpcService>> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void handleRequest(JsonRpcRequestMessage request, Builder replyBuilder) {
        final Optional<InboundHandler<RpcService>> handler = handlers.stream()
                .filter(h -> h.hasMethod(request.getMethod()))
                .findFirst();
        if (handler.isPresent()) {
            handler.orElseThrow().handleRequest(request, replyBuilder);
        } else {
            replyBuilder.error(new JsonRpcErrorObject(-32601, "No such method : " + request.getMethod(), null));
        }
    }
}
