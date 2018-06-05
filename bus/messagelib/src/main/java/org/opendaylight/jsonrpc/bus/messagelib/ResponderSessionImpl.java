/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import java.util.List;
import java.util.Objects;

import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.PeerContext;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcBaseMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcBaseMessage.JsonRpcMessageType;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcMessageError;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage.Builder;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcSerializer;

/**
 * Implementation of {@link ResponderSession}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 24, 2018
 */
public class ResponderSessionImpl extends AbstractSession implements MessageListener, ResponderSession {
    private final RequestMessageHandler handler;

    public ResponderSessionImpl(CloseCallback closeCallback, BusSessionFactory factory, RequestMessageHandler handler,
            String uri) {
        super(closeCallback);
        setAutocloseable(factory.responder(uri, this));
        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public void onMessage(PeerContext peerContext, String message) {
        final List<JsonRpcBaseMessage> incomming = JsonRpcSerializer.fromJson(message);
        try {
            PeerContextHolder.set(peerContext);
            for (final JsonRpcBaseMessage msg : incomming) {
                if (msg.getType() == JsonRpcMessageType.REQUEST) {
                    final Builder replyBuilder = JsonRpcReplyMessage.builder().id(msg.getId());
                    handler.handleRequest((JsonRpcRequestMessage) msg, replyBuilder);
                    peerContext.send(JsonRpcSerializer.toJson(replyBuilder.build()));
                } else {
                    peerContext.send(JsonRpcSerializer.toJson(JsonRpcMessageError.builder()
                            .code(-32600)
                            .message("Unexpected message type : " + msg.getType())
                            .build()));
                    return;
                }
            }
        } finally {
            PeerContextHolder.remove();
        }
    }
}
