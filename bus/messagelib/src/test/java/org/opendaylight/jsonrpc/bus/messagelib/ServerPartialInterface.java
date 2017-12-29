/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

public interface ServerPartialInterface extends AutoCloseable {

    String echo(String msg);

    String concat(String msg1, String msg2);

    String join(String delim, String[] msgs);

    void noReturn(String msg);

    String delayedEcho(String msg, int time);

    int increment(int count);

    /* Option can be used to throw different exceptions.*/
    void returnError(int option) throws Exception;

    @Override
    void close();
}
