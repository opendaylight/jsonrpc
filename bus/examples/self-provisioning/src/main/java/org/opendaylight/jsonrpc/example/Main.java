/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.example;

import com.google.gson.JsonObject;

import java.util.concurrent.CountDownLatch;

import org.opendaylight.jsonrpc.bus.messagelib.DefaultTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.ResponderSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.impl.AbstractSelfProvisionedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends AbstractSelfProvisionedService implements DemoService {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            LOG.error("Endpoint required");
            System.exit(0);
        }
        try (Main service = new Main()) {
            service.run(args[0]);
        }
    }

    private void run(String endpoint) throws Exception {
        try (TransportFactory tf = new DefaultTransportFactory();
                ResponderSession responder = tf.createResponder(endpoint, new Main())) {
            // wait forever
            new CountDownLatch(1).await();
        }
    }

    @Override
    public JsonObject factorial(JsonObject input) {
        final JsonObject ret = new JsonObject();
        int out = 1;
        for (int i = 2; i <= input.get("in-number").getAsInt(); i++) {
            out *= i;
        }
        ret.addProperty("out-number", out);
        return ret;
    }
}
