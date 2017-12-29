/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus;

/**
 * Message handler invoked by messaging library.
 */
@FunctionalInterface
public interface BusSessionMsgHandler {
    /**
     * Invoked to handle a message.
     *
     * @return 0 to continue loop, -1 to stop loop.
     */
    int handleIncomingMsg(String message);
}
