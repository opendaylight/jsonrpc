/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

/**
 * A generic Exception thrown by Message Library.
 *
 * @author Shaleen Saxena
 */
public class MessageLibraryException extends Exception {
    private static final long serialVersionUID = -8361140234723755252L;

    public MessageLibraryException(String message) {
        super(message);
    }

    public MessageLibraryException(Throwable cause) {
        super(cause);
    }
}
