/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import org.apache.commons.lang.StringUtils;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcException;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.opendaylight.jsonrpc.bus.messagelib.NotificationMessageHandler;
import org.opendaylight.jsonrpc.bus.messagelib.ReplyMessageHandler;
import org.opendaylight.jsonrpc.bus.messagelib.RequestMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMessageHandler implements RequestMessageHandler, ReplyMessageHandler, NotificationMessageHandler, ServerPartialInterface {
    private static final Logger logger = LoggerFactory.getLogger(TestMessageHandler.class);
    public String result;
    public JsonRpcErrorObject error;
    public String noticeMethod;
    public String noticeParam;
    private Lock lock = null;

    public TestMessageHandler() {
        // do nothing
    }

    public TestMessageHandler(Lock lock) {
        this();
        this.lock = lock;
    }

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
        logger.debug("Sleeping for {}", time);
        try {
        	Thread.sleep(time);
        } catch (InterruptedException e) {
        	logger.error("Got interrupted", e);
        	Thread.currentThread().interrupt();
        }
        return msg;
    }

    @Override
    public int increment(int in) {
        return ++in;
    }

    @Override
    public void close() {
        Thread.currentThread().interrupt();
        return;
    }
    
    @Override
    public void handleRequest(JsonRpcRequestMessage request,
            JsonRpcReplyMessage reply) {
        String method = request.getMethod();

        try {
            if (method.equals("echo")) {
                String params = request.getParamsAsObject(String.class);
                String res = echo(params);
                reply.setResultAsObject(res);
            } else if (method.equals("concat")) {
                String[] params = request.getParamsAsObject(String[].class);
                reply.setResultAsObject(concat(params[0], params[1]));
            } else if (method.equals("join")) {
                String delim = request
                        .getParamsAtIndexAsObject(0, String.class);
                String[] args = request.getParamsAtIndexAsObject(1,
                        String[].class);
                reply.setResultAsObject(join(delim, args));
            } else if (method.equals("noReturn")) {
                String params = request.getParamsAsObject(String.class);
                noReturn(params);
            } else if (method.equals("delayedEcho")) {
                String msg = request.getParamsAtIndexAsObject(0, String.class);
                int delay = request.getParamsAtIndexAsObject(1, int.class);
                String res = delayedEcho(msg, delay);
                reply.setResultAsObject(res);
            } else if (method.equals("increment")) {
                int incount = request.getParamsAtIndexAsObject(0, int.class);
                int outcount = increment(incount);
                reply.setResultAsObject(outcount);

            } else if (method.equals("close")) {
                close();
            } else {
                JsonRpcErrorObject error = new JsonRpcErrorObject(-32601,
                        "Method not found", null);
                reply.setError(error);
            }
        } catch (JsonRpcException e) {
            JsonRpcErrorObject error = new JsonRpcErrorObject(-32602,
                    "Invalid Params", null);
            reply.setError(error);
        }
    }

    @Override
    public void handleReply(JsonRpcReplyMessage reply) {
        try {
            this.result = reply.getResultAsObject(String.class);
            this.error = reply.getError();
        } catch (JsonRpcException e) {
            logger.error("Unable to parse reply", e);
            this.result = null;
        }
    }

    @Override
    public void handleNotification(JsonRpcRequestMessage notification) {
        try {
            this.noticeMethod = notification.getMethod();
            this.noticeParam = notification.getParamsAsObject(String.class);
        } catch (JsonRpcException e) {
            logger.error("Unable to parse notification", e);
            this.result = null;
        }
        if (lock != null) {
            lock.doNotify();
        }
    }
}
