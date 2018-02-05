/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper/utility class. Some of methods provided by this utility class are not
 * meant to be used directly, or just during tests.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public final class Util {
    public static final int DEFAULT_TIMEOUT = 30000;
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);
    private static final MapJoiner QUERY_JOINER = Joiner.on('&').withKeyValueSeparator("=");
    private static final String ROLE = "role";

    private Util() {
        // noop
    }

    /**
     * Trim schema (protocol) and use "tcp" in URI, remove any recognized
     * parameters and pass result to underlying transport library.
     *
     * @param inUri inbound URI
     * @return prepared URI
     */
    public static String prepareUri(URI inUri) {
        try {
            return trimTrailingQuestionMark(new URI("tcp", null, inUri.getHost(), inUri.getPort(), inUri.getPath(),
                    removeParams(inUri.getQuery() == null ? "" : inUri.getQuery(), ROLE), inUri.getFragment())
                            .toString());
        } catch (URISyntaxException e) {
            // Impossible, outbound URI is constructed from inbound with no
            // violation with regards to RFC2396
            throw new IllegalStateException("This should never happen", e);
        }
    }

    /**
     * Removes given query parameters from URI query string. If array of parameter names to remove is empty, original
     * query string is returned instead.
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
        Map<String, String> map = params.entrySet().stream()
                .filter(e -> !Arrays.asList(paramsToRemove).contains(e.getKey().trim()))
                // collect into LinkedHashMap, which preserves insertion order
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                    (first, second) -> first, LinkedHashMap::new));
        return QUERY_JOINER.join(map);
    }

    /**
     * Takes URI's query parameters part and replaces given query parameter with
     * new value. If no such parameter exists, it is added.
     *
     * @param rawQuery raw URI
     * @param paramName name of parameter to replace
     * @param paramValue value of replaced parameter
     * @return modified URI
     */
    public static String replaceParam(String rawQuery, String paramName, String paramValue) {
        final Map<String, String> params = Maps.newLinkedHashMap(tokenizeQuery(rawQuery));
        params.put(paramName, paramValue);
        Map<String, String> map = params.entrySet().stream().filter(e -> e.getValue() != null)
                // collect into LinkedHashMap, which preserves insertion order
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                    (first, second) -> first, LinkedHashMap::new));
        return QUERY_JOINER.join(map);
    }

    /**
     * ZMQ does not like ending '?', which is permitted by URI specification.
     */
    private static String trimTrailingQuestionMark(String uri) {
        return uri.endsWith("?") ? uri.substring(0, uri.length() - 1) : uri;
    }

    /**
     * <strong>This method is meant to be used by custom
     * TransportFactory.</strong>
     * Open {@link Session} to service at given URI. If query parameters within
     * URI didn't contain 'role' parameter, then roleStr argument will be used.
     *
     * @param messageLibrary the {@link MessageLibrary}
     * @param uri Service URI
     * @param roleStr Role, @see {@link EndpointRole}
     * @return {@link Session}
     * @throws IllegalArgumentException if given role is not recognized
     */
    public static Session openSession(MessageLibrary messageLibrary, URI uri, String roleStr) {
        final Map<String, String> params = tokenizeQuery(uri.getQuery() == null ? "" : uri.getQuery());
        // get role from URI or use default if not provided
        final EndpointRole role = EndpointRole.valueOf(!params.containsKey(ROLE)
                ? Objects.requireNonNull(roleStr, "No role specified in URI or argument").trim().toUpperCase()
                        : params.get(ROLE));
        final String preparedUri = prepareUri(uri);
        LOG.debug("Prepared URI : '{}', original URI : {}", preparedUri, uri);
        if (EndpointRole.REP.equals(role)) {
            return messageLibrary.responder(preparedUri);
        }
        if (EndpointRole.REQ.equals(role)) {
            return messageLibrary.requester(preparedUri);
        }
        if (EndpointRole.PUB.equals(role)) {
            return messageLibrary.publisher(preparedUri);
        }
        if (EndpointRole.SUB.equals(role)) {
            return messageLibrary.subscriber(preparedUri);
        }

        throw new IllegalArgumentException("Unrecognized role");
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

        public static Map<String, String> tokenize(String uri) {
            final Map<String, String> ret = new LinkedHashMap<>();
            final Iterable<String> paramTokens = PARAM_SPLITTER.split(uri);
            for (final String tok : paramTokens) {
                String[] parts = tok.split("=");
                if (!"".equals(parts[0])) {
                    ret.put(parts[0], parts.length == 2 ? parts[1] : null);
                }
            }
            LOG.trace("Tokenized : {} into {}", uri, ret);
            return ret;
        }
    }

    static Map<String, String> tokenizeQuery(String rawUri) {
        return UriTokenizer.tokenize(rawUri);
    }
}
