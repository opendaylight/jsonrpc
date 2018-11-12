/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.api;

/**
 * Exception is thrown by message library or transport implementation when condition is detected that can't be
 * recovered from (like re-try limit exceeded).
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Nov 12, 2018
 */
public class UnrecoverableTransportException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UnrecoverableTransportException(String error) {
        super(error);
    }

    public UnrecoverableTransportException(String error, Throwable ex) {
        super(error, ex);
    }
}
