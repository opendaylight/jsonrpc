/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMessageServer implements ServerPartialInterface {
    public static final Logger logger = LoggerFactory.getLogger(TestMessageServer.class);

    @Override
    public String echo(String msg) {
        return msg;
    }

    @Override
    public String concat(String msg1, String msg2) {
        return msg1 + msg2;
    }

    @Override
    public String join(String delim, String[] msgs) {
        return StringUtils.join(msgs, delim);
    }

    @Override
    public void noReturn(String msg) {
        // Do nothing function
        return;
    }

    @Override
    public String delayedEcho(String msg, int time) {
        // This method will go off to sleep for specified time.
        // Lets client test timeout
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            logger.error("Got interrupted", e);
            Thread.currentThread().interrupt();
        }
        return msg;
    }

    @Override
    public int increment(int count) {
        return ++count;
    }

    @Override
    public void close() {
        Thread.currentThread().interrupt();
        return;
    }
}
