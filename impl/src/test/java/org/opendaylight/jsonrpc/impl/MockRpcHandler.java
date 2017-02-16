/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.opendaylight.jsonrpc.bus.messagelib.RequestMessageHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.TestModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

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
            try (final InputStream is = Resources.getResource(MockRpcHandler.class, "/get-all-numbers-response.json")
                    .openStream()) {
                ByteStreams.copy(is, baos);
                MOCK_RESP_JSON = (JsonObject) PARSER.parse(baos.toString(Charsets.UTF_8.name()));
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public void handleRequest(JsonRpcRequestMessage request, JsonRpcReplyMessage response) {
        LOG.info("Received request : {}", request);

        switch (request.getMethod()) {
        case "simple-method":
            // no-op
            break;

        case "multiply-ll":
            processMultiplyLeafList(request.getParams(), response);
            break;

        case "multiply-list":
            processMultiplyList(request.getParams(), response);
            break;

        case "get-all-numbers":
            processGetAllNumbers(request.getParams(), response);
            break;

        case "error-method":
            response.setError(new JsonRpcErrorObject(12345, "It just failed", new JsonObject()));
            break;

        case "factorial":
            processFactorial(request.getParams(), response);
            break;

        case "method-with-anyxml":
            processAnyXml(request.getParams(), response);
            break;

        case "get-any-xml":
            processGetAnyXml(request.getParams(), response);
            break;

        case "removeCoffeePot":
            processRemoveCoffeePot(request.getParams(), response);
            break;

        default:
            // we should not land here
            break;
        }
        LOG.info("Sending response : {}", response);
    }

    private void processRemoveCoffeePot(JsonElement params, JsonRpcReplyMessage response) {
        response.setResult(PARSER.parse("{\"drink\": \"coffee\", \"cups-brewed\": 3}"));
    }

    private void processGetAnyXml(JsonElement params, JsonRpcReplyMessage response) {
        response.setResult(PARSER.parse("{\"outdata\": { \"data\" : 42 }}"));
    }

    private void processAnyXml(JsonElement params, JsonRpcReplyMessage response) {
        response.setResult(JsonNull.INSTANCE);
    }

    private void processFactorial(JsonElement params, JsonRpcReplyMessage response) {
        int in = params.getAsJsonObject().get("in-number").getAsInt();
        int out = 1;
        for (int i = 2; i <= in; i++) {
            out *= i;
        }
        JsonObject o = new JsonObject();
        o.addProperty("out-number", out);
        response.setResult(o);
    }

    private void processGetAllNumbers(JsonElement params, JsonRpcReplyMessage response) {
        response.setResult(MOCK_RESP_JSON);
    }

    private void processMultiplyList(JsonElement params, JsonRpcReplyMessage response) {
        JsonObject in = params.getAsJsonObject();
        int multiplier = in.get("multiplier").getAsInt();
        int[] numbers = intArrayfromJsonArray(in.get("numbers").getAsJsonArray());
        JsonObject o = new JsonObject();
        JsonArray arr = new JsonArray();
        for (int n : numbers) {
            JsonObject jo = new JsonObject();
            jo.addProperty("num", n * multiplier);
            arr.add(jo);
        }
        o.add("numbers", arr);
        response.setResult(o);
    }

    private void processMultiplyLeafList(JsonElement params, JsonRpcReplyMessage response) {
        JsonObject in = params.getAsJsonObject();
        int multiplier = in.get("multiplier").getAsInt();
        int[] numbers = intArrayfromJsonArray(in.get("numbers").getAsJsonArray());
        JsonObject o = new JsonObject();
        JsonArray arr = new JsonArray();
        for (int n : numbers) {
            arr.add(new JsonPrimitive(n * multiplier));
        }
        o.add("numbers", arr);
        response.setResult(o);
    }

    private static int[] intArrayfromJsonArray(JsonArray arr) {
        int[] ret = new int[arr.size()];
        int i = 0;
        for (JsonElement je : arr) {
            if (je instanceof JsonPrimitive) {
                ret[i++] = je.getAsInt();
            } else {
                ret[i++] = ((JsonObject) je).get("num").getAsInt();
            }
        }
        return ret;
    }

    @Override
    public void close() throws Exception {
        // no-op
    }
}
