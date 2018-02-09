/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.api;

/**
 * Checked {@link Exception} which is thrown by messaging library when message
 * was not received within timeout interval.
 *
 */
public class BusSessionTimeoutException extends Exception {
    private static final long serialVersionUID = 2035358626828577604L;

    public BusSessionTimeoutException(String msg) {
        super(msg);
    }
}
