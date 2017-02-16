/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import org.opendaylight.jsonrpc.bus.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.SessionType;
import org.zeromq.ZContext;

/**
 * This class implements the
 * {@link org.opendaylight.jsonrpc.bus.BusSessionFactory
 * BusSessionFactory} for the ZeroMQ bus service.
 * 
 * @author Shaleen Saxena
 *
 */
public class ZMQFactory implements BusSessionFactory<ZMQSession> {
    private final ZContext zmqContext;

    public ZMQFactory() {
        zmqContext = new ZContext();
    }

    public ZContext getZMQContext() {
        return zmqContext;
    }

    @Override
    public ZMQSession responder(String uri) {
        return new ZMQSession(zmqContext, uri, SessionType.RESPONDER);
    }

    @Override
    public ZMQSession requester(String uri) {
        return new ZMQSession(zmqContext, uri, SessionType.REQUESTER);
    }

    @Override
    public ZMQSession publisher(String uri, String topic) {
        return new ZMQSession(zmqContext, uri, SessionType.PUBLISHER, topic);
    }

    @Override
    public ZMQSession subscriber(String uri, String topic) {
        return new ZMQSession(zmqContext, uri, SessionType.SUBSCRIBER, topic);
    }

    @Override
    public void close() {
        zmqContext.destroy();
    }

    @Override
    public String name() {
        return "zmq";
    }
}
