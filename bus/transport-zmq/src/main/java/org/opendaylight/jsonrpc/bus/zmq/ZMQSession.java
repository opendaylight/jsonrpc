/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import org.opendaylight.jsonrpc.bus.BusSession;
import org.opendaylight.jsonrpc.bus.BusSessionMsgHandler;
import org.opendaylight.jsonrpc.bus.BusSessionTimeoutException;
import org.opendaylight.jsonrpc.bus.SessionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZLoop;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;

/**
 * This class implements the
 * {@link org.opendaylight.jsonrpc.bus.BusSession BusSession} interface
 * for the ZeroMQ bus service.
 *
 * @author Shaleen Saxena
 */
public class ZMQSession implements BusSession {
    private static final Logger LOG = LoggerFactory.getLogger(ZMQSession.class);
    private static final int DEFAULT_TIMEOUT = 30 * 1000; // 30 seconds

    private final ZContext zmqContext;
    private final URI uri;
    private final int socketType;
    private final byte[] topic;

    private Socket socket = null;
    private boolean opened = false;
    private int timeout;
    private Poller rxPoller;
    private Poller txPoller;
    private final SessionType sessionType;
    private Socket loopTransmit = null;

    public ZMQSession(ZContext zmqContext, String uri, SessionType sessionType) {
        this(zmqContext, uri, sessionType, "");
    }

    public ZMQSession(ZContext zmqContext, String uri, SessionType sessionType, String topic) {
        this.zmqContext = zmqContext;
        this.sessionType = sessionType;
        this.topic = topic.getBytes(StandardCharsets.UTF_8);
        this.uri = convertToUri(uri);

        // Set other fields.
        this.socketType = convertToSocketType(sessionType);
        setTimeoutToDefault();
        open();
    }

    private static URI convertToUri(String uri) {

        if (uri.endsWith("/")) {
            uri = uri.substring(0, uri.lastIndexOf('/'));
        }

        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            LOG.error("Invalid URI: " + uri, e);
            throw new IllegalArgumentException(e);
        }
    }

    private int convertToSocketType(SessionType type) {
        switch (type) {
            case REQUESTER:
                return ZMQ.REQ;
            case RESPONDER:
                return ZMQ.REP;
            case SUBSCRIBER:
                return ZMQ.SUB;
            case PUBLISHER:
                return ZMQ.PUB;
            default:
                throw new IllegalArgumentException("Unsupported session type: " + type);
        }
    }

    private void open() {
        if (!this.opened) {
            this.socket = zmqContext.createSocket(this.socketType);
            if (this.socketType == ZMQ.REQ) {
                this.socket.connect(this.uri.toString());
                createReceivePoller();
                createTransmitPoller();
            } else if (this.socketType == ZMQ.SUB) {
                this.socket.setRcvHWM(4);
                this.socket.setTCPKeepAlive(0);
                this.socket.connect(this.uri.toString());
                this.socket.subscribe(topic);
                createReceivePoller();
            } else if (this.socketType == ZMQ.PUB
                    || this.socketType == ZMQ.REP) {
                this.socket.bind(this.uri.toString());
                createTransmitPoller();
            } else {
                throw new IllegalArgumentException("Unknown socket type");
            }
            this.opened = true;
        }
    }

    @Override
    public void close() {
        if (socket != null) {
            socket.close();
            socket = null;
            opened = false;
        }
    }

    public void reopen() {
        close();
        open();
    }

    private void createReceivePoller() {
        rxPoller = new Poller(1);
        rxPoller.register(socket, Poller.POLLIN | Poller.POLLERR);
    }

    private void createTransmitPoller() {
        txPoller = new Poller(1);
        txPoller.register(socket, Poller.POLLOUT | Poller.POLLERR);
    }

    @Override
    public String readMessage() throws BusSessionTimeoutException {
        String message = null;
        try {
            if (rxPoller == null) {
                message = recvMessage();
            } else {
                // poll socket for a reply, with timeout
                if (rxPoller.poll(getTimeout()) != 1) {
                    LOG.debug("Receive interrupted");
                    reopen(); // reopen so that we can send again
                    String msg = String.format("Receive timed out: %d ms", getTimeout());
                    throw new BusSessionTimeoutException(msg);
                }
                if (rxPoller.pollerr(0)) {
                    LOG.debug("Receive errored");
                } else if (rxPoller.pollin(0)) {
                    message = recvMessage();
                }
            }
            LOG.debug("Received: " + message);
        } catch (ZMQException e) {
            LOG.error("Unable to read message", e);
        }
        return message;
    }

    private String recvMessage() {
        StringBuilder builder = new StringBuilder(socket.recvStr());
        while (socket.hasReceiveMore()) {
            builder.append(socket.recvStr());
        }
        // Trim topic from start of message
        builder.delete(0, topic.length);
        return builder.toString();
    }

    private void transmitMessage(String message) {
        if (socketType == ZMQ.PUB) {
            socket.sendMore(topic);
        }
        socket.send(message, 0);
    }

    @Override
    public boolean sendMessage(String message) {
        try {
            if (txPoller == null) {
                transmitMessage(message);
                return true;
            } else {
                // poll socket to be empty, with timeout
                if (txPoller.poll(getTimeout()) != 1) {
                    LOG.debug("Send interrupted");
                    return false; // Interrupted
                }
                if (txPoller.pollerr(0)) {
                    LOG.debug("Send errored");
                    return false;
                } else if (txPoller.pollout(0)) {
                    LOG.debug("Sending: " + message);
                    transmitMessage(message);
                    return true;
                }
            }
        } catch (ZMQException e) {
            LOG.error("Unable to send message", e);
        }
        return false;
    }

    @Override
    public void startLoop(final BusSessionMsgHandler handler) {
        Socket loopReceive;

        // sanity checks
        if (handler == null) {
            throw new IllegalArgumentException("Null handler");
        }
        if (socketType != ZMQ.SUB && socketType != ZMQ.REP) {
            throw new UnsupportedOperationException(
                    "This socket type not supported");
        }

        // Create internal ZMQ Pair for stopping the loop.
        // The inproc URI needs to be unique within the ZMQ context only.
        // Hence using Thread name, as there should not be more than one
        // ZLoop per thread.
        String loopUri = "inproc://" + Thread.currentThread().getName();
        loopReceive = zmqContext.createSocket(ZMQ.PAIR);
        loopReceive.bind(loopUri);

        loopTransmit = zmqContext.createSocket(ZMQ.PAIR);
        loopTransmit.connect(loopUri);

        // Create a new loop
        ZLoop loop = new ZLoop();
        // loop.verbose(true)

        // Add poller for main socket
        PollItem item1 = new PollItem(socket, ZMQ.Poller.POLLIN | ZMQ.Poller.POLLERR);
        loop.addPoller(item1,
            (ZLoop zloop, PollItem item, Object arg) -> {
                String msg = recvMessage();
                LOG.debug("Received: {}", msg);
                return msg.length() > 0 ? handler.handleIncomingMsg(msg) : 0;
            },
            null);

        // Add poller for loop control socket
        PollItem item2 = new PollItem(loopReceive, ZMQ.Poller.POLLIN | ZMQ.Poller.POLLERR);
        loop.addPoller(item2,
            (zloop, item, arg) -> -1, // stop on receiving any message
            null);

        // Wait in a loop and process messages
        loop.start();

        // Done with loop; cleanup
        loop.destroy();
        loopReceive.close();
        loopTransmit.close();
        loopTransmit = null;
    }

    @Override
    public void stopLoop() {
        // Tell loop to stop. Any message would do.
        if (loopTransmit != null) {
            loopTransmit.send("Stop".getBytes(StandardCharsets.UTF_8));
        }
    }

    public int getSocketType() {
        return socketType;
    }

    @Override
    public SessionType getSessionType() {
        return sessionType;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public void setTimeoutToDefault() {
        setTimeout(DEFAULT_TIMEOUT);
    }

    @Override
    public String toString() {
        return "ZMQSession [sessionType=" + sessionType + ", uri=" + uri + "]";
    }
}
