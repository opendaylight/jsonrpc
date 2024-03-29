/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.opendaylight.jsonrpc.bus.messagelib.MessageLibraryConstants.DEFAULT_PROXY_RETRY_COUNT;
import static org.opendaylight.jsonrpc.bus.messagelib.MessageLibraryConstants.DEFAULT_PROXY_RETRY_DELAY;
import static org.opendaylight.jsonrpc.bus.messagelib.MessageLibraryConstants.PARAM_PROXY_RETRY_COUNT;
import static org.opendaylight.jsonrpc.bus.messagelib.MessageLibraryConstants.PARAM_PROXY_RETRY_DELAY;

import com.google.common.collect.Queues;
import com.google.common.primitives.Ints;
import com.google.gson.JsonObject;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.PeerContext;
import org.opendaylight.jsonrpc.bus.api.RecoverableTransportException;
import org.opendaylight.jsonrpc.bus.api.Requester;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcBaseMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcBaseMessage.JsonRpcMessageType;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link RequesterSession}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 24, 2018
 */
public final class RequesterSessionImpl extends AbstractSession implements MessageListener, RequesterSession {
    private static final Logger LOG = LoggerFactory.getLogger(RequesterSessionImpl.class);
    private final Requester requester;
    private final ReplyMessageHandler handler;
    private final BlockingQueue<String> responseQueue = Queues.newLinkedBlockingDeque();
    private final AtomicReference<Future<?>> lastRequest = new AtomicReference<>(null);
    private final int retryCount;
    private final long retryDelay;

    public RequesterSessionImpl(Consumer<AutoCloseable> closeCallback, BusSessionFactory factory, String uri,
            ReplyMessageHandler handler) {
        super(closeCallback, uri);
        requester = factory.requester(uri, this);
        this.handler = Objects.requireNonNull(handler);
        retryCount = Ints.saturatedCast(Util.queryParamValue(uri, PARAM_PROXY_RETRY_COUNT, DEFAULT_PROXY_RETRY_COUNT));
        retryDelay = Util.queryParamValue(uri, PARAM_PROXY_RETRY_DELAY, DEFAULT_PROXY_RETRY_DELAY);
        setAutocloseable(requester);
    }

    @Override
    public void onMessage(PeerContext peerContext, String message) {
        LOG.debug("Response from {} : {}", peerContext.channel(), message);
        final List<JsonRpcBaseMessage> messages = JsonRpcSerializer.fromJson(message);
        try {
            PeerContextHolder.set(peerContext);
            for (final JsonRpcBaseMessage msg : messages) {
                if (msg.getType() == JsonRpcMessageType.REPLY) {
                    handler.handleReply((JsonRpcReplyMessage) msg);
                } else {
                    throw new MessageLibraryMismatchException(
                            String.format("Requester received %s message", msg.getType().name()));
                }
            }
        } finally {
            PeerContextHolder.remove();
        }
    }

    /**
     * A low level message sending method, to send user-created strings.
     *
     * @param msg A single message (i.e. request or reply)
     */
    private synchronized void send(final String message) {
        if (!responseQueue.isEmpty()) {
            throw new RecoverableTransportException("There is unfinished request on this channel, try again later");
        }
        LOG.debug("Sending request : {}", message);
        lastRequest.set(requester.send(message).addListener(new GenericFutureListener<Future<String>>() {
            @Override
            public void operationComplete(final Future<String> future) throws Exception {
                if (future.isSuccess()) {
                    responseQueue.add(future.get());
                } else {
                    LOG.warn("Send failed", future.cause());
                }
            }
        }));
    }

    @Override
    public String read() {
        try {
            final String resp = responseQueue.poll(timeout, TimeUnit.MILLISECONDS);
            if (resp == null) {
                lastRequest.getAndSet(null).cancel(true);
                throw new MessageLibraryTimeoutException(
                        String.format("Message was not received within %d milliseconds", timeout));
            } else {
                return resp;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private JsonRpcReplyMessage readReply(String msg) {
        final List<JsonRpcBaseMessage> replies = JsonRpcSerializer.fromJson(msg);
        if (replies.size() == 1) {
            if (replies.get(0).getType() != JsonRpcMessageType.REPLY) {
                throw new MessageLibraryMismatchException("Unexpected message : " + replies.get(0));
            } else {
                return (JsonRpcReplyMessage) replies.get(0);
            }
        } else {
            throw new MessageLibraryException("Unexpected number of replies (1 required) : " + replies.size());
        }
    }

    @Override
    public JsonRpcReplyMessage sendRequestAndReadReply(String name, Object object) {
        return sendRequestAndReadReply(name, object, null);
    }

    @Override
    public JsonRpcReplyMessage sendRequestAndReadReply(String name, Object object, JsonObject metadata) {
        sendRequest(name, object, metadata);
        return readReply(read());
    }

    @Override
    public void sendRequest(String method, Object params, JsonObject metadata) {
        sendMessage(JsonRpcRequestMessage.builder()
                .idFromIntValue(nextId())
                .method(method)
                .paramsFromObject(params)
                .metadata(metadata)
                .build());
    }

    @Override
    public void sendMessage(JsonRpcBaseMessage msg) {
        send(JsonRpcSerializer.toJson(msg));
    }

    @Override
    public void await() {
        requester.awaitConnection();
    }

    @Override
    public int retryCount() {
        return retryCount;
    }

    @Override
    public long retryDelay() {
        return retryDelay;
    }

    @Override
    public boolean isConnectionReady() {
        return requester.isReady();
    }
}
