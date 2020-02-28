/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcConstants.canRepresentJsonPrimitive;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcBaseRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper/utility class. Some of methods provided by this utility class are not
 * meant to be used directly, or just during tests.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public final class Util {
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);
    private static final MapJoiner QUERY_JOINER = Joiner.on('&').withKeyValueSeparator("=");

    private Util() {
        // noop
    }

    /**
     * Removes given query parameters from URI query string. If array of
     * parameter names to remove is empty, original query string is returned
     * instead.
     *
     * @param rawQuery query string from URI
     * @param paramsToRemove array of parameter names to remove
     * @return query string with removed parameters
     */
    public static String removeParams(String rawQuery, String... paramsToRemove) {
        // nothing to remove
        if (paramsToRemove == null || paramsToRemove.length == 0) {
            return rawQuery;
        }
        final Map<String, String> params = tokenizeQuery(rawQuery);
        Map<String, String> map = params.entrySet()
                .stream()
                .filter(e -> !Arrays.asList(paramsToRemove).contains(e.getKey().trim()))
                // collect into LinkedHashMap, which preserves insertion order
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first,
                        LinkedHashMap::new));
        return QUERY_JOINER.join(map);
    }

    /**
     * Originally I used Guava's {@link Splitter} and {@link Joiner} to
     * manipulate URI, but they don't like fact, that URI query parameter does
     * not require value, which is perfectly fine with RFC-2396.
     */
    @VisibleForTesting
    static final class UriTokenizer {
        private static final Splitter PARAM_SPLITTER = Splitter.on("&");

        private UriTokenizer() {
            // no instantiation here
        }

        /**
         * Parse query parameters into key-value mapping.
         *
         * @param queryParams query parameters from URI
         * @return key-value mapping of query parameters
         */
        public static Map<String, String> tokenize(String queryParams) {
            final Map<String, String> ret = new LinkedHashMap<>();
            final Iterable<String> paramTokens = PARAM_SPLITTER.split(queryParams != null ? queryParams : "");
            for (final String tok : paramTokens) {
                String[] parts = tok.split("=");
                if (!"".equals(parts[0])) {
                    ret.put(parts[0], parts.length == 2 ? parts[1] : null);
                }
            }
            LOG.trace("Tokenized : {} into {}", queryParams, ret);
            return ret;
        }
    }

    static Map<String, String> tokenizeQuery(String rawUri) {
        return UriTokenizer.tokenize(rawUri);
    }

    public static int getParametersCount(final JsonRpcBaseRequestMessage msg) {
        if (msg.getParams() instanceof JsonArray) {
            return ((JsonArray) msg.getParams()).size();
        }
        if (msg.getParams() instanceof JsonPrimitive) {
            return 1;
        }
        if (msg.getParams() instanceof JsonObject) {
            return 1;
        }
        return 0;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @SuppressFBWarnings("DE_MIGHT_IGNORE")
    public static void closeQuietly(AutoCloseable autoCloseable) {
        try {
            autoCloseable.close();
        } catch (Exception e) {
            // NOOP
        }
    }

    /**
     * Sorts list of matched methods based on preference. Currently it only
     * prefers method without underscore in it's name
     *
     * @return {@link Comparator}
     */
    public static Comparator<Method> nameSorter() {
        return (o1, o2) -> {
            if (o1.getName().contains("_")) {
                return 1;
            }
            if (o2.getName().contains("_")) {
                return -1;
            }
            return o1.getName().compareTo(o2.getName());
        };
    }

    /**
     * In order to have deterministic order of methods, we need to sort them by
     * argument types. This is because outcome of
     * {@link Class#getDeclaredMethods()} is not sorted.
     * @return {@link Comparator}
     */
    public static Comparator<Method> argsSorter() {
        return (left,
                right) -> Arrays.asList(left.getParameterTypes())
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.toList())
                        .hashCode()
                        - Arrays.asList(right.getParameterTypes())
                                .stream()
                                .map(Object::toString)
                                .collect(Collectors.toList())
                                .hashCode();
    }

    /**
     * Combination of {@link #nameSorter()} and {@link #argsSorter()}.
     * @return combined {@link Comparator}
     */
    public static Comparator<Method> nameAndArgsSorter() {
        return (left, right) -> ComparisonChain.start()
                .compare(left, right, argsSorter())
                .compare(left, right, nameSorter())
                .result();
    }

    /**
     * Parse query parameter value from URI or provide default value if not present.
     *
     * @param uri endpoint URI to parse value from
     * @param queryParamName query parameter name
     * @param defaultValue default value to use if not present
     * @return query parameter value
     */
    public static long queryParamValue(String uri, String queryParamName, long defaultValue) {
        try {
            final URI parsed = new URI(uri);
            return Long.parseLong(tokenizeQuery(parsed.getQuery()).computeIfAbsent(queryParamName,
                t -> String.valueOf(defaultValue)));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String injectQueryParam(String uri, String queryParamName, String queryParamValue) {
        try {
            final URI parsed = new URI(uri);
            final Map<String, String> params = tokenizeQuery(parsed.getQuery());
            params.put(queryParamName, queryParamValue);
            final StringBuilder sb = new StringBuilder();
            sb.append(parsed.getScheme()).append("://").append(parsed.getHost());
            if (parsed.getPort() != -1) {
                sb.append(':').append(parsed.getPort());
            }
            if (parsed.getPath() != null) {
                sb.append(parsed.getPath());
            }
            sb.append('?').append(QUERY_JOINER.join(params));
            return sb.toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Comparator that sorts {@link Method} according to payload type. It considers parameter type and {@link Method}
     * argument type. It is assumed that comparison is done on filtered method already (where method name and number of
     * arguments matched parameter count in payload).
     *
     * @param params message payload parameter
     * @return {@link Comparator}
     */
    public static Comparator<Method> payloadAwareSorter(JsonElement params) {
        return (left, right) -> {
            if (params == null || params.isJsonNull() || left.getParameterCount() == 0) {
                return 0;
            }
            if (params.isJsonPrimitive() && canRepresentJsonPrimitive(right.getParameterTypes()[0])) {
                return 1;
            }
            if (params.isJsonPrimitive() && canRepresentJsonPrimitive(left.getParameterTypes()[0])) {
                return -1;
            }
            if (params.isJsonObject() && !canRepresentJsonPrimitive(right.getParameterTypes()[0])) {
                return 1;
            }
            if (params.isJsonObject() && !canRepresentJsonPrimitive(left.getParameterTypes()[0])) {
                return -1;
            }
            return 0;
        };
    }

    /**
     * Await for underlying transport o become ready. This is needed when request is made, but transport not yet
     * finished handshake.
     *
     * @param session {@link ClientSession} to await for
     * @param milliseconds period to wait for (at most)
     */
    static void awaitForTransport(ClientSession session, long milliseconds) {
        final long future = System.currentTimeMillis() + milliseconds;
        while (System.currentTimeMillis() < future) {
            if (session.isConnectionReady()) {
                return;
            }
            Uninterruptibles.sleepUninterruptibly(100L, TimeUnit.MILLISECONDS);
        }
    }
}
