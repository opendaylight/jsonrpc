/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

/**
 * This is an exception that is thrown if the server returns an error in the
 * response. Currently the data field is ignored. The code can be obtained via
 * the message string.
 * 
 * @author Shaleen Saxena
 */
public class ProxyServiceGenericException extends RuntimeException {
    private static final long serialVersionUID = 7955465706793696600L;

    public ProxyServiceGenericException(String message) {
        super(message);
    }

    public ProxyServiceGenericException(String message, int code) {
        super("[code=" + String.valueOf(code) + "] " + message);
    }

    public ProxyServiceGenericException(Throwable throwable) {
        super(throwable);
    }
}
