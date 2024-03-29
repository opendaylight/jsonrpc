/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.kohsuke.MetaInfServices;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.Publisher;
import org.opendaylight.jsonrpc.bus.api.Requester;
import org.opendaylight.jsonrpc.bus.api.Responder;
import org.opendaylight.jsonrpc.bus.api.Subscriber;
import org.opendaylight.jsonrpc.bus.spi.AbstractBusSessionFactory;
import org.opendaylight.jsonrpc.bus.spi.EventLoopConfiguration;
import org.opendaylight.jsonrpc.security.noop.NoopSecurityService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;


/**
 * {@link BusSessionFactory} implemented using ZeroMQ 3.0 protocol.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 6, 2018
 */
@MetaInfServices(value = BusSessionFactory.class)
@Component(service = BusSessionFactory.class, property = "scheme=zmq")
public class ZmqBusSessionFactory extends AbstractBusSessionFactory {
    public ZmqBusSessionFactory() {
        super(Constants.TRANSPORT_NAME);
    }

    @Activate
    public ZmqBusSessionFactory(@Reference(target = "(name=jsonrpc)") EventLoopConfiguration config) {
        super(Constants.TRANSPORT_NAME, config, NoopSecurityService.INSTANCE);
    }

    @Deactivate
    public void deactivate() {
        close();
    }

    @Override
    public Publisher publisher(String uri) {
        final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        final Publisher publisher = new PublisherImpl(uri, serverBootstrap, channelGroup, handlerExecutor);
        addSession(publisher);
        return publisher;
    }

    @Override
    public Subscriber subscriber(String uri, String topic, MessageListener messageListener) {
        final Subscriber subscriber = new SubscriberImpl(uri, topic, messageListener, clientBootstrap, handlerExecutor);
        addSession(subscriber);
        return subscriber;
    }

    @Override
    public Requester requester(String uri, MessageListener listener) {
        final RequesterImpl requester = new RequesterImpl(uri, clientBootstrap, listener, handlerExecutor);
        addSession(requester);
        return requester;
    }

    @Override
    public Responder responder(String uri, MessageListener listener) {
        final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        final Responder responder = new ResponderImpl(uri, this.serverBootstrap, listener, channelGroup,
                handlerExecutor);
        addSession(responder);
        return responder;
    }
}
