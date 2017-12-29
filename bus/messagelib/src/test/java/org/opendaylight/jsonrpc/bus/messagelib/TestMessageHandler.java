/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcException;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMessageHandler implements RequestMessageHandler, ReplyMessageHandler, NotificationMessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TestMessageHandler.class);
    public String result;
    public JsonRpcErrorObject error;
    public String noticeMethod;
    public String noticeParam;
    private final TestMessageServer server;
    private Lock lock = null;

    public TestMessageHandler() {
        server = new TestMessageServer();
    }

    public TestMessageHandler(Lock lock) {
        this();
        this.lock = lock;
    }

    @Override
    public void handleRequest(JsonRpcRequestMessage request,
            JsonRpcReplyMessage reply) {
        String method = request.getMethod();

        try {
            if (method.equals("echo")) {
                String params = request.getParamsAsObject(String.class);
                String res = server.echo(params);
                reply.setResultAsObject(res);
            } else if (method.equals("concat")) {
                String[] params = request.getParamsAsObject(String[].class);
                reply.setResultAsObject(server.concat(params[0], params[1]));
            } else if (method.equals("join")) {
                String delim = request
                        .getParamsAtIndexAsObject(0, String.class);
                String[] args = request.getParamsAtIndexAsObject(1,
                        String[].class);
                reply.setResultAsObject(server.join(delim, args));
            } else if (method.equals("noReturn")) {
                String params = request.getParamsAsObject(String.class);
                server.noReturn(params);
            } else if (method.equals("delayedEcho")) {
                String msg = request.getParamsAtIndexAsObject(0, String.class);
                int delay = request.getParamsAtIndexAsObject(1, int.class);
                String res = server.delayedEcho(msg, delay);
                reply.setResultAsObject(res);
            } else if (method.equals("increment")) {
                int incount = request.getParamsAtIndexAsObject(0, int.class);
                int outcount = server.increment(incount);
                reply.setResultAsObject(outcount);

            } else if (method.equals("close")) {
                server.close();
            } else {
                reply.setError(new JsonRpcErrorObject(-32601, "Method not found", null));
            }
        } catch (JsonRpcException e) {
            reply.setError(new JsonRpcErrorObject(-32602, "Invalid Params", null));
        }
    }

    @Override
    public void handleReply(JsonRpcReplyMessage reply) {
        try {
            this.result = reply.getResultAsObject(String.class);
            this.error = reply.getError();
        } catch (JsonRpcException e) {
            LOG.error("Unable to parse reply", e);
            this.result = null;
        }
    }

    @Override
    public void handleNotification(JsonRpcRequestMessage notification) {
        try {
            this.noticeMethod = notification.getMethod();
            this.noticeParam = notification.getParamsAsObject(String.class);
        } catch (JsonRpcException e) {
            LOG.error("Unable to parse notification", e);
            this.result = null;
        }
        if (lock != null) {
            lock.doNotify();
        }
    }
}
