/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

public class Lock {

    private Object lock = new Object();
    private boolean inProgress = false;

    public void reset() {
        inProgress = true;
    }

    public void doWait() throws InterruptedException {
        synchronized (lock) {
            while (inProgress) {
                lock.wait();
            }
        }
    }

    public void doNotify() {
        synchronized (lock) {
            inProgress = false;
            lock.notify();
        }
    }
}
