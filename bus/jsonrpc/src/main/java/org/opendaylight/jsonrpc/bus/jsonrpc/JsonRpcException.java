/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

/**
 * This exceptions class represents the errors might occur while using this
 * package.
 * 
 * @author Shaleen Saxena
 */
public class JsonRpcException extends Exception {
    private static final long serialVersionUID = -6275941555460039587L;

    public JsonRpcException(String message) {
        super(message);
    }

    public JsonRpcException(Throwable cause) {
        super(cause);
    }
}
