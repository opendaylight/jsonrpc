/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.opendaylight.jsonrpc.bus.messagelib.MessageLibraryConstants.DEFAULT_PROXY_RETRY_COUNT;
import static org.opendaylight.jsonrpc.bus.messagelib.MessageLibraryConstants.DEFAULT_PROXY_RETRY_DELAY;
import static org.opendaylight.jsonrpc.bus.messagelib.MessageLibraryConstants.DEFAULT_TIMEOUT;
import static org.opendaylight.jsonrpc.bus.messagelib.MessageLibraryConstants.PARAM_PROXY_RETRY_COUNT;
import static org.opendaylight.jsonrpc.bus.messagelib.MessageLibraryConstants.PARAM_PROXY_RETRY_DELAY;
import static org.opendaylight.jsonrpc.bus.messagelib.MessageLibraryConstants.PARAM_TIMEOUT;

import java.net.URISyntaxException;

/**
 * Fluent builders to simply creation of endpoints.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Nov 16, 2018
 */
public final class EndpointBuilders {
    private EndpointBuilders() {
        // no instantiation of this class
    }

    public static class EndpointBuilder {
        private AbstractTransportFactory factory;

        EndpointBuilder(AbstractTransportFactory factory) {
            this.factory = factory;
        }

        /**
         * Create new builder for {@link RequesterSession} or requester proxy.
         *
         * @return {@link RequesterBuilder} instance
         */
        public RequesterBuilder requester() {
            return new RequesterBuilder(factory);
        }

        public ResponderBuilder responder() {
            return new ResponderBuilder(factory);
        }

        public PublisherBuilder publisher() {
            return new PublisherBuilder(factory);
        }

        public SubscriberBuilder subscriber() {
            return new SubscriberBuilder(factory);
        }
    }

    /**
     * Base of other endpoint builders.
     */
    public abstract static class BaseEndpointBuilder<T extends BaseEndpointBuilder<?>> {
        protected final AbstractTransportFactory factory;
        protected boolean useCache = false;

        BaseEndpointBuilder(AbstractTransportFactory factory) {
            this.factory = factory;
        }

        /**
         * Enable endpoint cache (which is disabled by default).
         *
         * @return this builder instance
         */
        @SuppressWarnings("unchecked")
        public T useCache() {
            useCache = true;
            return (T) this;
        }
    }

    /**
     * Builder for {@link RequesterSession} or requester proxy.
     */
    public static final class RequesterBuilder extends BaseEndpointBuilder<RequesterBuilder> {
        private int proxyRetryCount = DEFAULT_PROXY_RETRY_COUNT;
        private long proxyRetryDelay = DEFAULT_PROXY_RETRY_DELAY;
        private long requestTimeout = DEFAULT_TIMEOUT;

        private RequesterBuilder(AbstractTransportFactory factory) {
            super(factory);
        }

        /**
         * Configure request timeout.
         *
         * @param requestTimeoutMilliseconds request timeout value in milliseconds
         * @return this builder instance
         */
        public RequesterBuilder withRequestTimeout(long requestTimeoutMilliseconds) {
            this.requestTimeout = requestTimeoutMilliseconds;
            return this;
        }

        /**
         * Configure proxy retry parameters.
         *
         * @param retryCount number of retries before giving-up
         * @param delayMiliseconds delay between retries, in milliseconds
         * @return this builder instance
         */
        public RequesterBuilder withProxyConfig(int retryCount, long delayMiliseconds) {
            this.proxyRetryCount = retryCount;
            this.proxyRetryDelay = delayMiliseconds;
            return this;
        }

        /**
         * Create requester proxy using given API contract and URI.
         *
         * @param api API contract to create proxy for
         * @param uri remote responder endpoint
         * @param <T> API contract class
         * @return proxy object of given API class
         * @throws URISyntaxException if URI is invalid
         */
        public <T extends AutoCloseable> T createProxy(Class<T> api, String uri) throws URISyntaxException {
            String modified = uri;
            modified = Util.injectQueryParam(modified, PARAM_PROXY_RETRY_COUNT, String.valueOf(proxyRetryCount));
            modified = Util.injectQueryParam(modified, PARAM_PROXY_RETRY_DELAY, String.valueOf(proxyRetryDelay));
            modified = Util.injectQueryParam(modified, PARAM_TIMEOUT, String.valueOf(requestTimeout));
            return factory.createRequesterProxy(api, modified, !useCache);
        }

        /**
         * Create {@link RequesterSession} using given {@link ReplyMessageHandler} and URI.
         *
         * @param uri remote responder endpoint
         * @param handler handler used to handle responses
         * @return {@link RequesterSession}
         * @throws URISyntaxException if URI is invalid
         */
        public RequesterSession create(String uri, ReplyMessageHandler handler) throws URISyntaxException {
            return factory.createRequester(uri, handler, !useCache);
        }
    }

    /**
     * Builder for {@link RequesterSession} or requester proxy.
     */
    public static final class PublisherBuilder extends BaseEndpointBuilder<PublisherBuilder> {
        private PublisherBuilder(AbstractTransportFactory factory) {
            super(factory);
        }

        /**
         * Create publisher proxy.
         *
         * @param api API contract of publisher
         * @param uri local endpoint to bind to
         * @param <T> API contract type
         * @return proxy for given API
         * @throws URISyntaxException if URI is invalid
         */
        public <T extends AutoCloseable> T createProxy(Class<T> api, String uri) throws URISyntaxException {
            return factory.createPublisherProxy(api, uri, !useCache);
        }
    }

    /**
     * Builder of {@link SubscriberSession}.
     */
    public static final class SubscriberBuilder extends BaseEndpointBuilder<SubscriberBuilder> {
        private SubscriberBuilder(AbstractTransportFactory factory) {
            super(factory);
        }

        /**
         * Create new {@link SubscriberSession} using provided implementation and endpoint.
         *
         * @param uri remote endpoint to subscribe to
         * @param handler handler that will be invoked on incoming notification
         * @param <T> type of handler
         * @return {@link SubscriberSession}
         * @throws URISyntaxException if URI is invalid
         */
        public <T extends AutoCloseable> SubscriberSession createSubscriber(String uri, T handler)
                throws URISyntaxException {
            return factory.createSubscriber(uri, handler, !useCache);
        }
    }

    /**
     * Builder of {@link ResponderSession}.
     */
    public static final class ResponderBuilder extends BaseEndpointBuilder<ResponderBuilder> {
        private ResponderBuilder(AbstractTransportFactory factory) {
            super(factory);
        }

        /**
         * Create {@link ResponderBuilder} using provided instance and local endpoint to bound to.
         *
         * @param uri URI to bound to
         * @param handler service instance that will be invoked on incoming request
         * @param <T> type of handler
         * @return {@link ResponderSession}
         * @throws URISyntaxException if URI is invalid
         */
        public <T extends AutoCloseable> ResponderSession create(String uri, T handler) throws URISyntaxException {
            return factory.createResponder(uri, handler, !useCache);
        }
    }
}
