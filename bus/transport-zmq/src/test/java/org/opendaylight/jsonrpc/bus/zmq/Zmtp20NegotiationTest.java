/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import static org.junit.Assert.assertTrue;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.ProgressivePromise;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.PeerContext;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.spi.AbstractSessionTest;
import org.opendaylight.jsonrpc.bus.spi.CommonConstants;
import org.opendaylight.jsonrpc.bus.spi.DiscardingMessageListener;

/**
 * Tests negotiation with ZMTP 2.0 peers.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Apr 19, 2018
 */
public class Zmtp20NegotiationTest implements MessageListener {
    private final AtomicReference<ProgressivePromise<String>> currentRequest = new AtomicReference<>(null);
    private EventLoopGroup group;
    private ServerBootstrap sb;
    private Bootstrap cb;
    private CountDownLatch latch;

    @Before
    public void setUp() {
        group = new NioEventLoopGroup(4);
        sb = new ServerBootstrap().group(group).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.attr(CommonConstants.ATTR_SOCKET_TYPE).set(SessionType.REP);
                ch.attr(CommonConstants.ATTR_HANDSHAKE_DONE).set(false);
                ch.attr(Constants.ATTR_REMOTE_PEER).set(new PeerContextImpl(ch));
                ch.pipeline().addLast(Constants.HANDLER_HANDSHAKE, new HandshakeHandler((byte) 2));
                ch.pipeline().addLast(Constants.HANDLER_ENCODER, new MessageEncoder());
                ch.pipeline().addLast(Constants.HANDLER_DECODER, new MessageDecoder());
                ch.pipeline().addLast(group, CommonConstants.HANDLER_LISTENER,
                        new ServerHandler(Zmtp20NegotiationTest.this));
            }
        }).channel(NioServerSocketChannel.class);
        cb = new Bootstrap().group(group).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.attr(CommonConstants.ATTR_SOCKET_TYPE).set(SessionType.REQ);
                ch.attr(CommonConstants.ATTR_HANDSHAKE_DONE).set(false);
                ch.attr(CommonConstants.ATTR_RESPONSE_QUEUE).set(currentRequest);
                ch.attr(Constants.ATTR_REMOTE_PEER).set(new PeerContextImpl(ch));
                ch.pipeline().addLast(Constants.HANDLER_HANDSHAKE, new HandshakeHandler((byte) 2));
                ch.pipeline().addLast(Constants.HANDLER_DECODER, new MessageDecoder());
                ch.pipeline().addLast(Constants.HANDLER_ENCODER, new MessageEncoder());
                ch.pipeline().addLast(group, Constants.HANDLER_CLIENT,
                        new ClientHandler(DiscardingMessageListener.INSTANCE));
            }
        }).channel(NioSocketChannel.class);
        latch = new CountDownLatch(1);
    }

    @After
    public void tearDown() {
        group.shutdownGracefully();
    }

    @Test(timeout = 15_000)
    public void test() throws InterruptedException {
        final int port = AbstractSessionTest.getFreeTcpPort();
        final ChannelFuture responderCf = sb.bind(port).syncUninterruptibly();
        final ChannelFuture requesterCf = cb.connect("localhost", port).syncUninterruptibly();

        while (!requesterCf.channel().attr(CommonConstants.ATTR_HANDSHAKE_DONE).get()) {

        }

        requesterCf.channel().writeAndFlush(Util.serializeMessage("TEST")).syncUninterruptibly();
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        responderCf.channel().close().syncUninterruptibly();
        requesterCf.channel().close().syncUninterruptibly();
    }

    @Override
    public void onMessage(PeerContext peerContext, String message) {
        latch.countDown();
    }
}
