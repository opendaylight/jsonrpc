/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import com.google.gson.JsonObject;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcBaseMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;

public interface RequesterSession extends ClientSession {

    /**
     * A low level message to read messages of the bus. The return value would
     * probably need to be deserialized.
     *
     * @return The message as a String.
     * @throws MessageLibraryTimeoutException If this underlying bus receive
     *             time out while waiting for a message.
     */
    String read();

    /**
     * This method is used to send request message.
     *
     * @param method The method to which this request or notification is
     *            directed at.
     * @param params Optional parameters. Can be a single object or an array.
     * @param metadata Optional metadata. Should be a single object
     */
    void sendRequest(String method, Object params, JsonObject metadata);

    /**
     * A low level message sending method, to send user-created messages.
     *
     * @param msg A single message
     */
    void sendMessage(JsonRpcBaseMessage msg);

    /**
     * This method is used to send requests.
     *
     * @param name The method to which this request or notification is directed
     *            at.
     * @param object Optional parameter.
     * @return {@link JsonRpcReplyMessage}
     */
    JsonRpcReplyMessage sendRequestAndReadReply(String name, Object object);

    /**
     * This method is used to send requests with additional metadata.
     *
     * @param name The method to which this request or notification is directed
     *            at.
     * @param object Optional parameter.
     * @param metadata additional metadata
     * @return List of {@link JsonRpcReplyMessage}s
     */
    JsonRpcReplyMessage sendRequestAndReadReply(String name, Object object, JsonObject metadata);

    /**
     * Get number of retry attempts after request is considered failed.
     *
     * @return number of retry attempts
     */
    int retryCount();

    /**
     * Get configured retry delay between two request invocations.
     *
     * @return configured retry delay
     */
    long retryDelay();
}
