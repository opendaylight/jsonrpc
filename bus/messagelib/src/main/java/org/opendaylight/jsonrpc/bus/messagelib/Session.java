/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opendaylight.jsonrpc.bus.BusSession;
import org.opendaylight.jsonrpc.bus.BusSessionMsgHandler;
import org.opendaylight.jsonrpc.bus.BusSessionTimeoutException;
import org.opendaylight.jsonrpc.bus.SessionType;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcBaseMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcBaseMessage.JsonRpcMessageType;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The session object that is used to communicate over a bus. This object can be
 * of different types {@link SessionType}, which have different capabilities.
 * Use the {@link MessageLibrary} to create the correct session.
 *
 * @author Shaleen Saxena
 */
public class Session implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(Session.class);
    private final MessageLibrary messaging;
    private final SessionType sessionType;
    private final BusSession busSession;
    private final Object lock = new Object();
    private int id;
    private RequestMessageHandler requestMessageHandler;
    private ReplyMessageHandler replyMessageHandler;
    private NotificationMessageHandler notificationMessageHandler;

    Session(MessageLibrary messaging, BusSession busSession) {
        this.id = 0; // starting value
        this.messaging = messaging;
        this.sessionType = Objects.requireNonNull(busSession.getSessionType());
        this.busSession = busSession;
        messaging.add(this);
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public int getTimeout() {
        return busSession.getTimeout();
    }

    public void setTimeout(int time) {
        busSession.setTimeout(time);
    }

    public void setTimeoutToDefault() {
        busSession.setTimeoutToDefault();
    }

    @Override
    public void close() {
        messaging.remove(this);
        busSession.close();
    }

    private int newId() {
        return ++id;
    }

    /**
     * This method is used to send requests or notification messages. If the
     * channel is PUBLISHER, the message is assumed to be notification. If the
     * channel is REQUESTER, the message is assumed to be request.
     * @param method The method to which this request or notification is
     *            directed at.
     * @param params Optional parameters. Can be a single object or an array.
     * @throws MessageLibraryMismatchException If this is called for a session
     *             that does not support sending messges.
     */
    public void sendRequest(String method, Object params) throws MessageLibraryMismatchException {
        // Check if we have valid session
        if (sessionType == SessionType.SUBSCRIBER || sessionType == SessionType.RESPONDER) {
            throw new MessageLibraryMismatchException("Send not supported for this session.");
        }

        // Build Request
        JsonRpcRequestMessage request = new JsonRpcRequestMessage();
        request.setDefaultJsonrpc();
        request.setMethod(method);

        if (sessionType != SessionType.PUBLISHER) {
            request.setIdAsIntValue(newId());
        }

        if (params != null) {
            request.setParamsAsObject(params);
        }

        sendMessage(request);

        return;
    }

    /**
     * This method is used to send requests or notification messages. If the
     * channel is PUBLISHER, the message is assumed to be notification. If the
     * channel is REQUESTER, the message is assumed to be request.
     *
     * @param method The method to which this request or notification is
     *            directed at.
     * @param params Optional parameters. Can be a single object or an array.
     * @param metadata Optional metadata. Should be a single object
     * @throws MessageLibraryMismatchException If this is called for a session
     *             that does not support sending messges.
     */
    public void sendRequest(String method, Object params, JsonObject metadata) throws MessageLibraryMismatchException {
        // Check if we have valid session
        if (sessionType == SessionType.SUBSCRIBER || sessionType == SessionType.RESPONDER) {
            throw new MessageLibraryMismatchException("Send not supported for this session.");
        }

        // Build Request
        JsonRpcRequestMessage request = new JsonRpcRequestMessage();
        request.setDefaultJsonrpc();
        request.setMethod(method);

        if (sessionType != SessionType.PUBLISHER) {
            request.setIdAsIntValue(newId());
        }

        if (params != null) {
            request.setParamsAsObject(params);
        }

        if (metadata != null) {
            request.setMetadata(metadata);
        }
        sendMessage(request);

        return;
    }

    /**
     * Sends a reply message with a result.
     *
     * @param idElem The id of the request which generated this reply.
     * @param result The result object.
     * @throws MessageLibraryMismatchException see exception for more details
     */
    public void sendReplyWithResult(JsonElement idElem, Object result) throws MessageLibraryMismatchException {
        // Build Result Reply
        JsonRpcReplyMessage reply = new JsonRpcReplyMessage();
        reply.setDefaultJsonrpc();
        reply.setId(idElem);
        reply.setResultAsObject(result);
        sendMessage(reply);
    }

    /**
     * Sends a reply message with a result, provide metadata support.
     *
     * @param idElem The id of the request which generated this reply.
     * @param result The result object.
     * @param metadata The metadata object.
     * @throws MessageLibraryMismatchException see exception for more details
     */
    public void sendReplyWithResult(JsonElement idElem, Object result, JsonObject metadata)
            throws MessageLibraryMismatchException {
        // Build Result Reply
        JsonRpcReplyMessage reply = new JsonRpcReplyMessage();
        reply.setDefaultJsonrpc();
        reply.setId(idElem);
        reply.setResultAsObject(result);
        reply.setMetadata(metadata);
        sendMessage(reply);
    }

    /**
     * Sends a reply message with an error response.
     *
     * @param idElem The id of the request which generated this reply.
     * @param error The error containing code, message, and optional data.
     * @throws MessageLibraryMismatchException see exception for more details
     */
    public void sendReplyWithError(JsonElement idElem, JsonRpcErrorObject error)
            throws MessageLibraryMismatchException {
        // Build Error Reply
        JsonRpcReplyMessage reply = new JsonRpcReplyMessage();
        reply.setDefaultJsonrpc();
        reply.setId(idElem);
        reply.setError(error);
        sendMessage(reply);
    }

    /**
     * A low level message sending method, to send user-created strings.
     *
     * @param msg A single message (i.e. request or reply)
     * @throws MessageLibraryMismatchException see exception for more details
     */
    public void sendMessage(String msg) throws MessageLibraryMismatchException {
        if (sessionType == SessionType.SUBSCRIBER) {
            throw new MessageLibraryMismatchException("Send not supported for session.");
        }

        synchronized (lock) {
            busSession.sendMessage(msg);
        }
    }

    /**
     * A low level message sending method, to send user-created messages.
     *
     * @param msg A single message (i.e. request or reply)
     * @throws MessageLibraryMismatchException see exception for more details
     */
    public void sendMessage(JsonRpcBaseMessage msg) throws MessageLibraryMismatchException {
        sendMessage(JsonRpcSerializer.toJson(msg));
    }

    /**
     * A low level message sending method, to send a list of user-created messages.
     *
     * @param msg
     *            A list of messages (i.e. request or reply).
     * @throws MessageLibraryMismatchException see exception for more details
     */
    public void sendMessage(List<JsonRpcBaseMessage> msg) throws MessageLibraryMismatchException {
        if (msg.isEmpty()) {
            // nothing to do
            return;
        } else if (msg.size() == 1) {
            // Send message a single object
            sendMessage(JsonRpcSerializer.toJson(msg.get(0)));
        } else {
            // Send message as a bulk (i.e. array)
            sendMessage(JsonRpcSerializer.toJson(msg));
        }
    }

    /**
     * A low level message to read messages of the bus. The return value would
     * probably need to be deserialized.
     *
     * @return The message as a String.
     * @throws MessageLibraryMismatchException If this is called for session that
     *             does not support reads.
     * @throws MessageLibraryTimeoutException If this underlying bus receive time
     *             out while waiting for a message.
     */
    public String readMessage() throws MessageLibraryException {
        String msg;

        // Check if we have valid session
        if (sessionType == SessionType.PUBLISHER) {
            throw new MessageLibraryMismatchException("Receive not supported for session.");
        }

        synchronized (lock) {
            try {
                msg = busSession.readMessage();
            } catch (BusSessionTimeoutException e) {
                throw new MessageLibraryTimeoutException(e);
            }
        }

        return msg;
    }

    public String sendRequestAndReadReply(String name, Object object) throws MessageLibraryException {
        synchronized (lock) {
            sendRequest(name, object);
            if (sessionType == SessionType.PUBLISHER) {
                return null;
            }
            return readMessage();
        }
    }

    public void setRequestMessageHandler(RequestMessageHandler requestMessageHandler) {
        this.requestMessageHandler = requestMessageHandler;
    }

    public RequestMessageHandler getRequestMessageHandler() {
        return requestMessageHandler;
    }

    public void setReplyMessageHandler(ReplyMessageHandler replyMessageHandler) {
        this.replyMessageHandler = replyMessageHandler;
    }

    public ReplyMessageHandler getReplyMessageHandler() {
        return replyMessageHandler;
    }

    public void setNotificationMessageHandler(NotificationMessageHandler notificationMessageHandler) {
        this.notificationMessageHandler = notificationMessageHandler;
    }

    public NotificationMessageHandler getNotificationMessageHandler() {
        return notificationMessageHandler;
    }

    public void startLoop(BusSessionMsgHandler handler) {
        busSession.startLoop(handler);
    }

    public void stopLoop() {
        busSession.stopLoop();
    }

    public int processIncomingMessage(String message) throws MessageLibraryMismatchException {
        List<JsonRpcBaseMessage> incoming = JsonRpcSerializer.fromJson(message);
        List<JsonRpcBaseMessage> outgoing = new ArrayList<>();

        for (JsonRpcBaseMessage msg : incoming) {
            switch (sessionType) {
                case PUBLISHER:
                    // Publisher should not receive any message
                    throw new MessageLibraryMismatchException(
                        String.format("Publisher received %s message", msg.getType().name()));
                case REQUESTER:
                    requesterHandleMessage(msg);
                    break;
                case RESPONDER:
                    outgoing.add(responderHandleMessage(msg));
                    break;
                case SUBSCRIBER:
                    // Subscriber should get only Notification
                    subscriberHandleMessage(msg);
                    break;
                default:
                    break;
            }
        }

        // sendMessage() clears the interrupted state. So save it.
        boolean isThreadInterrupted = Thread.currentThread().isInterrupted();
        sendMessage(outgoing);
        if (isThreadInterrupted) {
            Thread.currentThread().interrupt();
        }

        return incoming.size();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private JsonRpcBaseMessage responderHandleMessage(JsonRpcBaseMessage msg) {
        if (msg.getType() == JsonRpcMessageType.PARSE_ERROR) {
            return msg;
        }
        JsonRpcReplyMessage reply = new JsonRpcReplyMessage();
        reply.setDefaultJsonrpc();
        reply.setId(msg.getId());
        // Responder should receive only Requests
        if (msg.getType() != JsonRpcMessageType.REQUEST) {
            JsonRpcErrorObject error = new JsonRpcErrorObject(-32600, "Invalid request", null);
            reply.setError(error);
        } else if (requestMessageHandler == null) {
            JsonRpcErrorObject error = new JsonRpcErrorObject(-32601, "Method not found", null);
            reply.setError(error);
        } else {
            try {
                requestMessageHandler.handleRequest((JsonRpcRequestMessage) msg, reply);
                if (!reply.isError() && !reply.isResult()) {
                    // handler didn't set a result, so set a dummy value.
                    reply.setResult(new JsonObject());
                }
            } catch (RuntimeException e) {
                LOG.error("Unable to handle request", e);
                JsonRpcErrorObject error = new JsonRpcErrorObject(-32603, "Internal error", null);
                reply.setError(error);
            }
        }
        return reply;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void subscriberHandleMessage(JsonRpcBaseMessage msg) throws MessageLibraryMismatchException {
        // Subscriber should only receive Notification
        if (msg.getType() != JsonRpcMessageType.NOTIFICATION) {
            throw new MessageLibraryMismatchException(
                    String.format("Requester received %s message", msg.getType().name()));
        }
        if (notificationMessageHandler == null) {
            // Looks like higher layers don't care about this message.
            return;
        }
        try {
            notificationMessageHandler.handleNotification((JsonRpcRequestMessage) msg);
        } catch (RuntimeException e) {
            LOG.error("Unable to handle notification", e);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void requesterHandleMessage(JsonRpcBaseMessage msg) throws MessageLibraryMismatchException {
        // Requester should only receive Reply
        if (msg.getType() != JsonRpcMessageType.REPLY) {
            throw new MessageLibraryMismatchException(
                    String.format("Requester received %s message", msg.getType().name()));
        }
        if (replyMessageHandler == null) {
            // Looks like higher layers don't care about this message.
            return;
        }
        try {
            replyMessageHandler.handleReply((JsonRpcReplyMessage) msg);
        } catch (RuntimeException e) {
            LOG.error("Unable to handle reply", e);
        }
    }

    /**
     * This will read an incoming message from the bus, parses it, and handles
     * the message as per its type. The incoming message could be an array of
     * batched messages, and they will be all handled sequentially.
     *
     * @return The number of JSON RPC messages it handled as a single bus
     *         message.
     * @throws MessageLibraryMismatchException If this is called for an incorrect
     */
    public int handleIncomingMessage() throws MessageLibraryException {
        String message = null;
        try {
            message = readMessage();
        } catch (MessageLibraryTimeoutException e) {
            LOG.error("Message read timed out", e);
            return 0;
        }

        if (message == null) {
            return 0;
        }

        return processIncomingMessage(message);
    }

    @Override
    public String toString() {
        return "Session [busSession=" + busSession + "]";
    }
}
