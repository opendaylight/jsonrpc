/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

public class TestMessageSubscriber implements PublishInterface {
    public String noticeMethod;
    public String noticeParam;
    private Lock lock;

    public TestMessageSubscriber(Lock lock) {
        this.lock = lock;
    }

    @Override
    public void publish(String msg) {
        this.noticeMethod = "publish";
        this.noticeParam = msg;
        if (lock != null) {
            lock.doNotify();
        }
    }

    @Override
    public void close() {
        // no-op
    }
}
