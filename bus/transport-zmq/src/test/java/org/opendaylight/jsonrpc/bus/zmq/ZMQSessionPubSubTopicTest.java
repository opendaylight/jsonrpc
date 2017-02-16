/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.BusSessionTimeoutException;
import org.opendaylight.jsonrpc.bus.zmq.ZMQFactory;
import org.opendaylight.jsonrpc.bus.zmq.ZMQSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZMQSessionPubSubTopicTest {
    private static final Logger logger = LoggerFactory.getLogger(ZMQSessionPubSubTopicTest.class);
    private static final String topic1 = "MyTopic";
    private static final String topic2 = "OtherTopic";

    private static ZMQFactory factory;
    private static ZMQSession pub;
    private static ZMQSession sub1;
    private static ZMQSession sub2;

    String msg1 = "Hello";
    static int timeout = 500; // 0.5 second wait

	static void showFunctionName() {
	    logger.info(Thread.currentThread().getStackTrace()[2].getMethodName());
	}

	@BeforeClass
    public static void setup() {
        showFunctionName();

        factory = new ZMQFactory();
        assertNotNull(factory);

        // Publisher for a topic
        String port = TestHelper.getFreeTcpPort();
        pub = factory.publisher("tcp://*:" + port, topic1);
        assertNotNull(pub);
        pub.setTimeout(timeout);

        // Subscriber 1 listens on publisher topic
        sub1 = factory.subscriber("tcp://127.0.0.1:" + port, topic1);
        assertNotNull(sub1);
        sub1.setTimeout(timeout);

        // Subscriber 2 listens on different topic
        sub2 = factory.subscriber("tcp://127.0.0.1:" + port, topic2);
        assertNotNull(sub2);
        sub2.setTimeout(timeout);

        // Wait for sub to actually join
        try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			logger.debug("Sleep interrupted", e);
			Thread.currentThread().interrupt();
		}
    }

    @Test
    public void pubSubTopicSendReceive() throws BusSessionTimeoutException 
    {
        showFunctionName();

        // Publish message
        pub.sendMessage(msg1);

        // Verify subscriber 1 gets message
        String rxMsg = sub1.readMessage();
        assertEquals(msg1, rxMsg);

        // Verify subscriber 2 got nothing
        try {
        	rxMsg = sub2.readMessage();
        	logger.info("recieved", rxMsg);
        	fail("Received a message");
        } catch (BusSessionTimeoutException e) {
        	assertTrue(true);
        }
    }

    @AfterClass
    public static void teardown() {
        showFunctionName();
        pub.close();
        sub1.close();
    }

}
