/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

public class JsonRpcConstants {
    public static final String JSONRPC = "jsonrpc";
    public static final String METHOD = "method";
    public static final String PARAMS = "params";
    public static final String ID = "id";
    public static final String RESULT = "result";
    public static final String ERROR = "error";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";
    public static final String DATA = "data";
    public static final String METADATA = "metadata";

    private JsonRpcConstants() {
        // do not call constructor
    }
}
