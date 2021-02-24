/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import java.net.URISyntaxException;

/**
 * Implementation of {@link TransportFactory} that overrides invocation of particular methods in
 * {@link AbstractTransportFactory} and delegates them to provided mock instance. This class is useful in unit testing
 * only.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jan 20, 2019
 */
public class MockTransportFactory extends AbstractTransportFactory {
    protected final AbstractTransportFactory delegate;

    public MockTransportFactory(AbstractTransportFactory delegate) {
        super(TcclBusSessionFactoryProvider.getInstance());
        this.delegate = delegate;
    }

    @Override
    public <T extends AutoCloseable> T createPublisherProxy(Class<T> clazz, String rawUri, boolean skipCache)
            throws URISyntaxException {
        return delegate.createPublisherProxy(clazz, rawUri, skipCache);
    }

    @Override
    public <T extends AutoCloseable> T createPublisherProxy(Class<T> clazz, String rawUri) throws URISyntaxException {
        return delegate.createPublisherProxy(clazz, rawUri);
    }

    @Override
    public <T extends AutoCloseable> T createRequesterProxy(Class<T> clazz, String rawUri, boolean skipCache)
            throws URISyntaxException {
        return delegate.createRequesterProxy(clazz, rawUri, skipCache);
    }

    @Override
    public <T extends AutoCloseable> T createRequesterProxy(Class<T> clazz, String rawUri) throws URISyntaxException {
        return delegate.createRequesterProxy(clazz, rawUri);
    }

    @Override
    public <T extends AutoCloseable> ResponderSession createResponder(String rawUri, T handler, boolean skipCache)
            throws URISyntaxException {
        return delegate.createResponder(rawUri, handler, skipCache);
    }

    @Override
    public <T extends AutoCloseable> ResponderSession createResponder(String rawUri, T handler)
            throws URISyntaxException {
        return delegate.createResponder(rawUri, handler);
    }

    @Override
    public <T extends AutoCloseable> SubscriberSession createSubscriber(String rawUri, T handler, boolean skipCache)
            throws URISyntaxException {
        return delegate.createSubscriber(rawUri, handler, skipCache);
    }

    @Override
    public <T extends AutoCloseable> SubscriberSession createSubscriber(String rawUri, T handler)
            throws URISyntaxException {
        return delegate.createSubscriber(rawUri, handler);
    }

    @Override
    public RequesterSession createRequester(String rawUri, ReplyMessageHandler handler, boolean skipCache)
            throws URISyntaxException {
        return delegate.createRequester(rawUri, handler, skipCache);
    }

    @Override
    public RequesterSession createRequester(String rawUri, ReplyMessageHandler handler) throws URISyntaxException {
        return delegate.createRequester(rawUri, handler);
    }
}
