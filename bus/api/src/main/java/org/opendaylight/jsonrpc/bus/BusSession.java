/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus;

/**
 * This interface defines a session that can be used to send or receive messages
 * over a bus. The messages are treated as Strings. More complex objects may
 * need to be serialized before sending or deserialized after receiving. This
 * interface needs to be implemented by various bus implementations.
 *
 * <p>
 * A timeout value may be specified. This is the time to wait before the bus is
 * available for sending or receiving messages.
 *
 * <p>
 * Use {@link #close()} to close this session.
 *
 * @author Shaleen Saxena
 *
 */
public interface BusSession extends AutoCloseable {

    /**
     * Read message from bus. This method will block caller until message is
     * received or timeout expired, whichever comes first
     *
     * @return message received from bus
     * @throws BusSessionTimeoutException when message was not received within
     *             timeout interval
     */
    String readMessage() throws BusSessionTimeoutException;

    /**
     * Send text message to bus.
     *
     * @param message message to send
     * @return true if and only if transmission of message was successful
     */
    boolean sendMessage(String message);

    /**
     * Get session type.
     *
     * @return {@link SessionType}
     */
    SessionType getSessionType();

    /**
     * Get current timeout value, in milliseconds.
     *
     * @return timeout value
     */
    int getTimeout();

    /**
     * Set timeout in milliseconds.
     *
     * @param timeout timeout value
     */
    void setTimeout(int timeout);

    /**
     * Reset timeout to default value, which is transport specific. Consult
     * actual value with transport implementation documentation.
     */
    void setTimeoutToDefault();

    /**
     * Start RX/TX loop.
     *
     * @param handler message handler used to process messages
     */
    void startLoop(BusSessionMsgHandler handler);

    /**
     * Stop RX/TX loop.
     */
    void stopLoop();

    /**
     * Performs cleanup of session.
     */
    @Override
    void close();
}
