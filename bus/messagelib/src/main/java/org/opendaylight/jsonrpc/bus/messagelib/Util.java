/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.opendaylight.jsonrpc.bus.SessionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;

/**
 * Helper/utility class. Some of methods provided by this utility class are not
 * meant to be used directly, or just during tests.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class Util {
    public static final int DEFAULT_TIMEOUT = 30000;
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);
    private static final MapJoiner QUERY_JOINER = Joiner.on('&').withKeyValueSeparator("=");
    private static final String ROLE = "role";
    // Simple cache to re-use MessageLibrary instances
    private static final LoadingCache<String, MessageLibrary> ML_CACHE = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, MessageLibrary>() {
                @Override
                public MessageLibrary load(String key) throws Exception {
                    return new MessageLibrary(key);
                }
            });

    // Cache also proxy instances so they can be reused
    private static final LoadingCache<MessageLibrary, ProxyService> PROXY_CACHE = CacheBuilder.newBuilder()
            .build(new CacheLoader<MessageLibrary, ProxyService>() {
                @Override
                public ProxyService load(MessageLibrary key) throws Exception {
                    return new ProxyServiceImpl(key);
                }
            });

    private Util() {
        // noop
    }

    /**
     * Same as {@link #createProxy(Class, String, int)}, but using
     * default timeout which is {@value #DEFAULT_TIMEOUT}.EndpointRole is
     * determined by 'role' query parameter, which is mandatory
     *
     * @param <T> API type to proxy, must implement {@link AutoCloseable}
     * @param clazz interface to create proxy against
     * @param rawUri URI pointing to service
     * @return proxy instance of T
     * @throws URISyntaxException when URI syntax is incorrect
     * @throws IllegalArgumentException when provided endpoint role is not
     *             allowed
     */
    public static <T extends AutoCloseable> T createProxy(Class<T> clazz, String rawUri) throws URISyntaxException {
        return createProxy(clazz, rawUri, DEFAULT_TIMEOUT);
    }

    /**
     * Same as {@link #createProxy(Class, String, int)}, but using build-in
     * {@link LoadingCache}
     *
     * @param <T> API type to proxy, must implement {@link AutoCloseable}
     * @param clazz interface to create proxy against
     * @param rawUri URI pointing to service
     * @param timeout connection timeout
     * @return proxy instance of T
     * @throws URISyntaxException when URI syntax is incorrect
     * @throws IllegalArgumentException when provided endpoint role is not
     *             allowed
     */
    public static <T extends AutoCloseable> T createProxy(Class<T> clazz, String rawUri, int timeout)
            throws URISyntaxException {
        return createProxy(ML_CACHE, clazz, rawUri, timeout);
    }

    /**
     * <strong>This method is meant to be used by custom
     * TransportFactory.</strong> 
     * Create proxy of given interface. Allowed endpoint roles are PUB and
     * REQ.EndpointRole is determined by 'role' query parameter, which is
     * mandatory. It also allows to use custom {@link LoadingCache}
     *
     * @param cache {@link LoadingCache} used to get/create instances of
     *            {@link MessageLibrary}
     * @param <T> API type to proxy, must implement {@link AutoCloseable}
     * @param clazz interface to create proxy against
     * @param rawUri URI pointing to service
     * @param timeout connection timeout
     * @return proxy instance of T
     * @throws URISyntaxException if URI syntax is incorrect
     * @throws IllegalArgumentException when provided endpoint role is not
     *             allowed
     */
    public static <T extends AutoCloseable> T createProxy(LoadingCache<String, MessageLibrary> cache, Class<T> clazz,
            String rawUri, int timeout) throws URISyntaxException {
        final URI uri = new URI(rawUri);
        final EndpointRole role = EndpointRole.valueOf(tokenizeQuery(uri.getQuery()).get("role"));
        final MessageLibrary ml = cache.getUnchecked(uri.getScheme());
        final ProxyService proxy = PROXY_CACHE.getUnchecked(ml);
        switch (role) {
        case PUB:
            return proxy.createPublisherProxy(prepareUri(uri), clazz, timeout);
        case REQ:
            return proxy.createRequesterProxy(prepareUri(uri), clazz, timeout);
        default:
            throw new IllegalArgumentException(String.format("Unrecognized endoint role : %s", role));
        }
    }

    /**
     * Create {@link ThreadedSession} of type {@link SessionType#RESPONDER} to
     * given URI
     *
     * @param <T> handler type
     * @param rawUri URI
     * @param handler used to handle requests
     * @return {@link ThreadedSession}
     * @throws URISyntaxException if URI syntax is incorrect
     */
    public static <T extends AutoCloseable> ThreadedSession createThreadedResponderSession(String rawUri, T handler)
            throws URISyntaxException {
        return createThreadedResponderSession(ML_CACHE, rawUri, handler);
    }

    /**
     * <strong>This method is meant to be used by custom
     * TransportFactory.</strong> 
     * Create {@link ThreadedSession} of type {@link SessionType#RESPONDER} to
     * given URI. It also allows to use custom {@link LoadingCache}
     *
     * @param cache {@link LoadingCache} used to get/create instances of
     *            {@link MessageLibrary}
     * @param <T> handler type
     * @param rawUri URI
     * @param handler used to handle requests
     * @return {@link ThreadedSession}
     * @throws URISyntaxException if URI syntax is incorrect
     */
    public static <T extends AutoCloseable> ThreadedSession createThreadedResponderSession(
            LoadingCache<String, MessageLibrary> cache, String rawUri, T handler) throws URISyntaxException {
        final URI uri = new URI(rawUri);
        return cache.getUnchecked(uri.getScheme()).threadedResponder(prepareUri(uri), handler);
    }

    /**
     * Create {@link ThreadedSession} of type {@link SessionType#SUBSCRIBER} to
     * given URI
     *
     * @param <T> handler type
     * @param rawUri URI
     * @param handler used to handle requests
     * @return {@link ThreadedSession}
     * @throws URISyntaxException if URI syntax is incorrect
     */
    public static <T extends AutoCloseable> ThreadedSession createThreadedSubscriberSession(String rawUri, T handler)
            throws URISyntaxException {
        return createThreadedSubscriberSession(ML_CACHE, rawUri, handler);
    }

    /**
     * <strong>This method is meant to be used by custom
     * TransportFactory.</strong> 
     * Create {@link ThreadedSession} of type {@link SessionType#SUBSCRIBER} to
     * given URI
     *
     * @param cache {@link LoadingCache} used to get/create instances of
     *            {@link MessageLibrary}
     * @param <T> handler type
     * @param rawUri URI
     * @param handler used to handle requests
     * @return {@link ThreadedSession}
     * @throws URISyntaxException if URI syntax is incorrect
     */
    public static <T extends AutoCloseable> ThreadedSession createThreadedSubscriberSession(
            LoadingCache<String, MessageLibrary> cache, String rawUri, T handler) throws URISyntaxException {
        final URI uri = new URI(rawUri);
        return cache.getUnchecked(uri.getScheme()).threadedSubscriber(prepareUri(uri), handler);
    }

    /**
     * Trim schema (protocol) and use "tcp" in URI, remove any recognized
     * parameters and pass result to underlying transport library
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
     * Removes given query parameters from URI query string. If array of
     * parameter names to remove is empty, original query string is returned
     * instead
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
        return QUERY_JOINER.join((Map<?, ?>) params.entrySet().stream()
                .filter(e -> !Arrays.asList(paramsToRemove).contains(e.getKey().trim()))
                // collect into LinkedHashMap, which preserves insertion order
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new)));
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
        return QUERY_JOINER.join((Map<?, ?>) params.entrySet().stream().filter(e -> e.getValue() != null)
                // collect into LinkedHashMap, which preserves insertion order
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new)));
    }

    /**
     * ZMQ does not like ending '?', which is permitted by URI specification
     */
    private static String trimTrailingQuestionMark(String uri) {
        return uri.endsWith("?") ? uri.substring(0, uri.length() - 1) : uri;
    }

    /**
     * Similar to {@link #openSession(String, String)}, but it is required here
     * that query already contains 'role' parameter, otherwise error will be
     * raised.
     *
     * @param rawUri Service URI
     * @return {@link Session}
     */
    public static Session openSession(String rawUri) {
        return openSession(rawUri, null);
    }

    /**
     * <strong>This method is meant to be used by custom
     * TransportFactory.</strong> 
     * Open {@link Session} to service at given URI. If query parameters within
     * URI didn't contain 'role' parameter, then roleStr argument will be used.
     *
     * @param cache {@link LoadingCache} used to get/create instances of
     *            {@link MessageLibrary}
     * @param rawUri Service URI
     * @param roleStr Role, @see {@link EndpointRole}
     * @return {@link Session}
     * @throws IllegalArgumentException if given role is not recognized
     */
    public static Session openSession(LoadingCache<String, MessageLibrary> cache, String rawUri, String roleStr) {
        try {
            final URI uri = new URI(rawUri);
            final Map<String, String> params = tokenizeQuery(uri.getQuery() == null ? "" : uri.getQuery());
            // get role from URI or use default if not provided
            final EndpointRole role = EndpointRole.valueOf(!params.containsKey(ROLE)
                    ? Objects.requireNonNull(roleStr.trim().toUpperCase(), "No role specified in URI or argument")
                    : params.get(ROLE));
            final String preparedUri = prepareUri(uri);
            LOG.debug("Prepared URI : '{}', original URI : {}", preparedUri, rawUri);
            final MessageLibrary ml = cache.getUnchecked(uri.getScheme());
            if (EndpointRole.REP.equals(role)) {
                return ml.responder(preparedUri);
            }
            if (EndpointRole.REQ.equals(role)) {
                return ml.requester(preparedUri);
            }
            if (EndpointRole.PUB.equals(role)) {
                return ml.publisher(preparedUri);
            }
            if (EndpointRole.SUB.equals(role)) {
                return ml.subscriber(preparedUri);
            }
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid URI", e);
        }
        throw new IllegalArgumentException("Unrecognized role");
    }

    /**
     * Open {@link Session} to service at given URI. If query parameters within
     * URI didn't contain 'role' parameter, then roleStr argument will be used.
     *
     * @param rawUri Service URI
     * @param roleStr Role, @see {@link EndpointRole}
     * @return {@link Session}
     * @throws IllegalArgumentException if given role is not recognized
     */
    public static Session openSession(String rawUri, String roleStr) {
        return openSession(ML_CACHE, rawUri, roleStr);
    }

    /**
     * Perform global transport factory cleanup, should be called when
     * application exists, only to ensure that all transports/sessions has been
     * cleaned-up
     */
    public static void close() {
        PROXY_CACHE.asMap().clear();
        ML_CACHE.asMap().values().spliterator().forEachRemaining(MessageLibrary::close);
        ML_CACHE.asMap().clear();
    }

    /**
     * Originally I used Guava's {@link Splitter} and {@link Joiner} to
     * manipulate URI, but they don't like fact, that URI query parameter does
     * not require value, which is perfectly fine with RFC-2396.
     */
    @VisibleForTesting
    static class UriTokenizer {
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

    private static Map<String, String> tokenizeQuery(String rawUri) {
        return UriTokenizer.tokenize(rawUri);
    }
}
