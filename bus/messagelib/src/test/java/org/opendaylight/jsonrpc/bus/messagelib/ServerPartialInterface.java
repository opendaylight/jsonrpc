/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

public interface ServerPartialInterface extends AutoCloseable {

    public String echo(String msg);

    public String concat(String msg1, String msg2);

    public String join(String delim, String[] msgs);
    
    public void noReturn(String msg);

    public String delayedEcho(String msg, int time);

    @Override
    public void close();
}
