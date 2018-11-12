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
import java.util.function.Consumer;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ResponderSession}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 24, 2018
 */
public class ResponderSessionImpl extends AbstractSession implements MessageListener, ResponderSession {
    private static final Logger LOG = LoggerFactory.getLogger(ResponderSessionImpl.class);
    private final RequestMessageHandler handler;

    public ResponderSessionImpl(Consumer<AutoCloseable> closeCallback, BusSessionFactory factory,
            RequestMessageHandler handler, String uri) {
        super(closeCallback, uri);
        setAutocloseable(factory.responder(uri, this));
        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public void onMessage(PeerContext peerContext, String message) {
        LOG.info("Request : {}", message);
        final List<JsonRpcBaseMessage> incomming = JsonRpcSerializer.fromJson(message);
        try {
            PeerContextHolder.set(peerContext);
            for (final JsonRpcBaseMessage msg : incomming) {
                if (msg.getType() == JsonRpcMessageType.REQUEST) {
                    final Builder replyBuilder = JsonRpcReplyMessage.builder().id(msg.getId());
                    handler.handleRequest((JsonRpcRequestMessage) msg, replyBuilder);
                    reply(peerContext, JsonRpcSerializer.toJson(replyBuilder.build()));
                } else {
                    reply(peerContext, JsonRpcSerializer.toJson(JsonRpcMessageError.builder()
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

    private void reply(PeerContext peer, String message) {
        LOG.info("Response : {}", message);
        peer.send(message);
    }
}
