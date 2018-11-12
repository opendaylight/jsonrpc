/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq.interop;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.PeerContext;
import org.opendaylight.jsonrpc.bus.api.Requester;

/**
 * Requester in java, responder in Python. Python code just reverse inbound
 * message and send it back.
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
 * socket = context.socket(zmq.REP)
 * socket.bind("tcp://*:%s" % port)
 *
 * while True:
 *     message = socket.recv()
 *     socket.send(message[::-1])
 *     time.sleep(1)
 * </pre>
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 27, 2018
 */
public class JavaReqPythonRep extends AbstractInteropTest {
    @Test
    public void test() throws InterruptedException, ExecutionException {
        final String in = "ABCD";
        final CountDownLatch latch = new CountDownLatch(1);
        final Requester req = factory.requester("zmq://0.0.0.0:10000", new MessageListener() {
            @Override
            public void onMessage(PeerContext peerContext, String message) {
                assertTrue(new StringBuilder(in).reverse().toString().equals(message));
                latch.countDown();
            }
        });
        req.awaitConnection();
        req.send(in).get();
        req.close();
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }
}
