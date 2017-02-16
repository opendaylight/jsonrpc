/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;

/**
 * This interface needs to be implemented by classes that wish to handle
 * requests. A response needed after handling any request. The reply object is
 * passed in along with the request. This reply object needs to be filled in
 * with either a result or error object
 * ({@link org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject
 * JsonRpcErrorObject}).
 * 
 * @author Shaleen Saxena
 */
@FunctionalInterface
public interface RequestMessageHandler {
    void handleRequest(JsonRpcRequestMessage request, JsonRpcReplyMessage reply);
}
