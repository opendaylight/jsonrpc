/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.io.Resources;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.opendaylight.jsonrpc.bus.messagelib.RequestMessageHandler;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link RequestMessageHandler} which acts as mock implementation of
 * {@link RemoteOmShard}.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class OmRootMessageHandler implements RequestMessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OmRootMessageHandler.class);
    private final int governancePort;

    public OmRootMessageHandler(int governancePort) {
        this.governancePort = governancePort;
    }

    @Override
    public void handleRequest(JsonRpcRequestMessage request, JsonRpcReplyMessage.Builder replyBuilder) {
        LOG.info("Req : {}", request);
        try {
            switch (request.getMethod()) {
                case "source":
                    replyBuilder.result(new JsonPrimitive(getYangSource(request.getParams().getAsString())));
                    return;
                case "governance":
                    replyBuilder.result(new JsonPrimitive(String.format("zmq://localhost:%d", governancePort)));
                    return;
                case "close":
                    replyBuilder.result(new JsonPrimitive("ok"));
                    return;
                default:
                    replyBuilder.resultFromObject("ERROR : unknown method : " + request.getMethod());
                    return;
            }
        } catch (IOException e) {
            LOG.error("I/O error", e);
            replyBuilder.resultFromObject("ERROR");
        }
    }

    private String getYangSource(String moduleName) throws IOException {
        String str = Resources.toString(Resources.getResource(getClass(), "/" + moduleName + ".yang"),
                StandardCharsets.US_ASCII);
        return str;
    }
}
