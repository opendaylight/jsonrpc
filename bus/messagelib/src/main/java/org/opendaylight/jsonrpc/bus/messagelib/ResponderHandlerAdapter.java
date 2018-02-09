/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcException;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Invocation adapter for {@link RequestMessageHandler}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @author Shaleen Saxena
 * @since Mar 27, 2018
 */
public class ResponderHandlerAdapter extends AbstractProxyHandlerAdapter implements RequestMessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ResponderHandlerAdapter.class);

    public ResponderHandlerAdapter(final Object handler) {
        super(false, handler);
    }

    /**
     * Handles JSON-RPC request. If handler instance provided in constructor is
     * instance of {@link RequestMessageHandler} then handling is delegated to
     * it instead.
     *
     * @see RequestMessageHandler#handleRequest(JsonRpcRequestMessage,
     *      JsonRpcReplyMessage.Builder)
     */
    @Override
    @SuppressWarnings({ "squid:S1166", "checkstyle:IllegalCatch" })
    public void handleRequest(JsonRpcRequestMessage request, JsonRpcReplyMessage.Builder replyBuilder) {
        if (handler instanceof RequestMessageHandler) {
            ((RequestMessageHandler) handler).handleRequest(request, replyBuilder);
            return;
        }
        try {
            Object response = invokeHandler(request);
            replyBuilder.resultFromObject(response);
        } catch (NoSuchMethodException e) {
            LOG.error("Request method not found: {}", request.getMethod());
            JsonRpcErrorObject error = new JsonRpcErrorObject(-32601, "Method not found", null);
            replyBuilder.error(error);
        } catch (JsonRpcException | IllegalArgumentException e) {
            LOG.error("Invalid arguments", e);
            JsonRpcErrorObject error = new JsonRpcErrorObject(-32602, "Invalid params", null);
            replyBuilder.error(error);
        } catch (Exception e) {
            LOG.error("Error while executing method: {}", request.getMethod(), e);
            JsonRpcErrorObject error = new JsonRpcErrorObject(-32000, getErrorMessage(e), null);
            replyBuilder.error(error);
        }
    }
}
