/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq.interop;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.PeerContext;
import org.opendaylight.jsonrpc.bus.api.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publisher in Python, subscriber in Java. Publisher simply emits "ABCD"
 * message in loop with some delay.
 *
 * <p>
 * Python code
 *
 * <pre>
 * import zmq
 * import time
 *
 * port = 10000
 *
 * context = zmq.Context()
 * socket = context.socket(zmq.PUB)
 * socket.bind("tcp://*:%s" % port)
 *
 * while True:
 *     socket.send("ABCD")
 *     time.sleep(1)
 *
 * </pre>
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 27, 2018
 */
public class PythonPubJavaSub extends AbstractInteropTest {
    private static final Logger LOG = LoggerFactory.getLogger(PythonPubJavaSub.class);

    @Test
    public void test() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(3);
        final Subscriber sub = factory.subscriber("zmq://0.0.0.0:10000", new MessageListener() {
            @Override
            public void onMessage(PeerContext peerContext, String message) {
                LOG.info("Got message : '{}'", message);
                latch.countDown();
            }
        });
        latch.await(10, TimeUnit.SECONDS);
        sub.close();
    }
}
