/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.api;

/**
 * Exception thrown by transport implementation when attempt is made to read/write data from/to channel which is not
 * ready. Caller can retry operation later.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Nov 12, 2018
 */
public class RecoverableTransportException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RecoverableTransportException(SessionType sessionType, String address) {
        super(String.format("Remote enpodint not ready : %s@%s", sessionType, address));
    }
}
