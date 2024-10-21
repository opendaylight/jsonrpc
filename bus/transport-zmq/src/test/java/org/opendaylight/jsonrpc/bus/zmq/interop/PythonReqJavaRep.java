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
import org.opendaylight.jsonrpc.bus.api.Responder;

/**
 * Requester in Python, responder in Java. Java code just reverse message and
 * send it back.
 *
 * <p>Python code
 * <pre>
 * import zmq
 * import time
 *
 * port = 10000
 * # Socket to talk to server
 * context = zmq.Context()
 * socket = context.socket(zmq.REQ)
 *
 * socket.connect("tcp://localhost:%s" % port)
 *
 * while True:
 *     msg = 'Hey!'
 *     socket.send(msg)
 *     message = socket.recv()
 *     assert message == msg[::-1]
 *     time.sleep(1)
 *
 * </pre>
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 27, 2018
 */
public class PythonReqJavaRep extends AbstractInteropTest {
    @Test
    public void test() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Responder rep = factory.responder("zmq://127.0.0.1:10000", new MessageListener() {
            @Override
            public void onMessage(PeerContext peerContext, String message) {
                peerContext.send(new StringBuilder(message).reverse().toString());
                latch.countDown();
            }
        });
        latch.await(10, TimeUnit.SECONDS);
        rep.close();
    }
}
