/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;

/**
 * This interface needs to be implemented by classes that wish to handle Reply
 * messages. There is no response needed after handling any reply.
 *
 * @author Shaleen Saxena
 */
public interface ReplyMessageHandler {
    void handleReply(JsonRpcReplyMessage reply);
}
