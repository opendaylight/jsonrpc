/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq.interop;

import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.api.Publisher;

/**
 * Publisher in Java, subscriber in Python. Publisher simply emits "ABCD"
 * message couple of times and then goes down.
 *
 * <p>Python code
 * <pre>
 * import sys
 * import zmq
 *
 * port = 10000
 * context = zmq.Context()
 * socket = context.socket(zmq.SUB)
 *
 * socket.connect ("tcp://localhost:%s" % port)
 * socket.setsockopt(zmq.SUBSCRIBE, '')
 *
 * while True:
 *     string = socket.recv()
 *     print string
 *
 * </pre>
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 27, 2018
 */
public class JavaPubPythonSub extends AbstractInteropTest {
    @Test
    public void test() {
        final Publisher pub = factory.publisher("zmq://0.0.0.0:10000");
        for (int i = 0; i < 3; i++) {
            pub.publish("ABCD");
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
        }
        pub.close();
    }
}
