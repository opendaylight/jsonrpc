/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.opendaylight.jsonrpc.bus.messagelib.RequestMessageHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.TestModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock RPC service implementation.
 *
 * @see TestModelService
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class MockRpcHandler implements RequestMessageHandler, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(MockRpcHandler.class);
    private static final JsonObject MOCK_RESP_JSON;
    private static final JsonParser PARSER = new JsonParser();

    static {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            try (InputStream is = Resources.getResource(MockRpcHandler.class, "/get-all-numbers-response.json")
                    .openStream()) {
                ByteStreams.copy(is, baos);
                MOCK_RESP_JSON = (JsonObject) PARSER.parse(baos.toString(StandardCharsets.UTF_8.name()));
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public void handleRequest(JsonRpcRequestMessage request, JsonRpcReplyMessage.Builder replyBuilder) {
        LOG.info("Received request : {}", request);

        switch (request.getMethod()) {
            case "simple-method":
                // no-op
                break;

            case "multiply-ll":
                processMultiplyLeafList(request.getParams(), replyBuilder);
                break;

            case "multiply-list":
                processMultiplyList(request.getParams(), replyBuilder);
                break;

            case "get-all-numbers":
                processGetAllNumbers(request.getParams(), replyBuilder);
                break;

            case "error-method":
                replyBuilder.error(new JsonRpcErrorObject(12345, "It just failed", new JsonObject()));
                break;

            case "factorial":
                processFactorial(request.getParams(), replyBuilder);
                break;

            case "method-with-anyxml":
                processAnyXml(request.getParams(), replyBuilder);
                break;

            case "get-any-xml":
                processGetAnyXml(request.getParams(), replyBuilder);
                break;

            case "removeCoffeePot":
                processRemoveCoffeePot(request.getParams(), replyBuilder);
                break;

            default:
                // we should not land here
                break;
        }
        LOG.info("Sending response : {}", replyBuilder);
    }

    private void processRemoveCoffeePot(JsonElement params, JsonRpcReplyMessage.Builder replyBuilder) {
        replyBuilder.result(PARSER.parse("{\"drink\": \"coffee\", \"cups-brewed\": 3}"));
    }

    private void processGetAnyXml(JsonElement params, JsonRpcReplyMessage.Builder replyBuilder) {
        replyBuilder.result(PARSER.parse("{\"outdata\": { \"data\" : 42 }}"));
    }

    private void processAnyXml(JsonElement params, JsonRpcReplyMessage.Builder replyBuilder) {
        replyBuilder.result(JsonNull.INSTANCE);
    }

    private void processFactorial(JsonElement params, JsonRpcReplyMessage.Builder replyBuilder) {
        int in = params.getAsJsonObject().get("in-number").getAsInt();
        int out = 1;
        for (int i = 2; i <= in; i++) {
            out *= i;
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("out-number", out);
        replyBuilder.result(obj);
    }

    private void processGetAllNumbers(JsonElement params, JsonRpcReplyMessage.Builder replyBuilder) {
        replyBuilder.result(MOCK_RESP_JSON);
    }

    private void processMultiplyList(JsonElement params, JsonRpcReplyMessage.Builder replyBuilder) {
        JsonObject in = params.getAsJsonObject();
        int multiplier = in.get("multiplier").getAsInt();
        int[] numbers = intArrayfromJsonArray(in.get("numbers").getAsJsonArray());
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();
        for (int n : numbers) {
            JsonObject jo = new JsonObject();
            jo.addProperty("num", n * multiplier);
            arr.add(jo);
        }
        obj.add("numbers", arr);
        replyBuilder.result(obj);
    }

    private void processMultiplyLeafList(JsonElement params, JsonRpcReplyMessage.Builder replyBuilder) {
        JsonObject in = params.getAsJsonObject();
        int multiplier = in.get("multiplier").getAsInt();
        int[] numbers = intArrayfromJsonArray(in.get("numbers").getAsJsonArray());
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();
        for (int n : numbers) {
            arr.add(new JsonPrimitive(n * multiplier));
        }
        obj.add("numbers", arr);
        replyBuilder.result(obj);
    }

    private static int[] intArrayfromJsonArray(JsonArray arr) {
        int[] ret = new int[arr.size()];
        int index = 0;
        for (JsonElement je : arr) {
            if (je instanceof JsonPrimitive) {
                ret[index++] = je.getAsInt();
            } else {
                ret[index++] = ((JsonObject) je).get("num").getAsInt();
            }
        }
        return ret;
    }

    @Override
    public void close() throws Exception {
        // no-op
    }
}
