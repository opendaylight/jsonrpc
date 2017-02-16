/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus;

/**
 * The type of communication channels that can be opened.
 *
 * @author Shaleen Saxena
 */
public enum SessionType {
    RESPONDER,
    REQUESTER,
    PUBLISHER,
    SUBSCRIBER,
}
