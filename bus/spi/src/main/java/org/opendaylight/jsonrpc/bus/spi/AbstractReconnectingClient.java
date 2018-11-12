/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.Uninterruptibles;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.opendaylight.jsonrpc.bus.api.ClientSession;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client endpoint which handles reconnect in case of failure.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 6, 2018
 */
public abstract class AbstractReconnectingClient extends AbstractSession implements ClientSession {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractReconnectingClient.class);
    private final Bootstrap clientBootstrap;
    private ScheduledFuture<?> reconnectFuture;
    protected volatile ConnectionState state = ConnectionState.INITIAL;
    private final ChannelFutureListener connectListener = new ConnectListener();
    private final ChannelFutureListener closeListener = new CloseListener();
    protected final AbstractChannelInitializer channelInitializer;
    private ReconnectStrategy reconnectStrategy;
    private final AtomicReference<Boolean> isFirstConnectionAttempt = new AtomicReference<>(true);
    private static final Set<ConnectionState> RECONNECT_STATES = ImmutableSet.<ConnectionState>builder()
            .add(ConnectionState.DONE)
            .build();

    public AbstractReconnectingClient(String uri, int defaultPort, Bootstrap clientBootstrap,
            AbstractChannelInitializer channelInitializer, SessionType sessionType) {
        super(uri, defaultPort, sessionType);
        reconnectStrategy = ReconnectStrategies.fixedStartegy(1000);
        this.channelInitializer = Objects.requireNonNull(channelInitializer);
        this.clientBootstrap = Objects.requireNonNull(clientBootstrap);
    }

    /*
     * If we are not yet done, then schedule reconnect
     */
    private void scheduleReconnect() {
        if (ConnectionState.DONE != state) {
            changeConnectionState(ConnectionState.INITIAL);
            reconnectFuture = clientBootstrap.config().group().schedule(this::connectInternal,
                    reconnectStrategy.timeout(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Attempts to connect to server.
     *
     * @see Channel#connect(java.net.SocketAddress)
     */
    protected void connectInternal() {
        if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) {
            return;
        }
        if (state == ConnectionState.DONE) {
            throw new IllegalStateException("Client closed already : " + address);
        }
        LOG.debug("(Re)connecting to {} ", address);
        changeConnectionState(ConnectionState.CONNECTING);
        clientBootstrap.handler(channelInitializer).connect(address).addListener(connectListener);
    }

    /**
     * Invoked when {@link Channel} is closed.
     */
    private class CloseListener implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (!RECONNECT_STATES.contains(state)) {
                LOG.debug("Scheduling reconnect because state is {}@{}", state, hashCode());
                changeConnectionState(ConnectionState.INITIAL);
                scheduleReconnect();
            }
        }
    }

    /**
     * Connection callback. Invoked when
     * <ul>
     * <li>Connection is normally established.</li>
     * <li>Connection attempt failed.</li>
     * </ul>
     *
     * @see Bootstrap#connect(java.net.SocketAddress)
     * @see ChannelFuture#addListener(io.netty.util.concurrent.GenericFutureListener)
     */
    private class ConnectListener implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                // Connection established
                channelFuture = future;
                changeConnectionState(ConnectionState.CONNECTED);
                reconnectStrategy.reset();
                future.channel().closeFuture().addListener(closeListener);
            } else {
                // log warning only for first connection failure
                if (isFirstConnectionAttempt.getAndSet(false)) {
                    LOG.warn("Connection attempt to '{}' failed", address, future.cause());
                } else {
                    LOG.trace("Connection attempt to '{}' failed", address, future.cause());
                }
                scheduleReconnect();
            }
        }
    }

    private void changeConnectionState(ConnectionState newState) {
        if (state != newState) {
            LOG.debug("Changing connection state from {} to {} [{}]@{}", state, newState,
                    channelFuture != null ? channelFuture.channel() : "N/A", hashCode());
            state = newState;
        }
    }

    /**
     * Signals that we are done and perform cleanup of client's {@link Channel}.
     *
     * @return {@link Future} which is completed once we are done with cleanup.
     */
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    protected java.util.concurrent.Future<Void> closeChannel() {
        changeConnectionState(ConnectionState.DONE);
        if (reconnectFuture != null) {
            reconnectFuture.cancel(true);
            reconnectFuture = null;
        }
        if (channelFuture != null) {
            return channelFuture.channel().close();
        }
        return Futures.immediateFuture(null);
    }

    /**
     * Check is client is ready for communication.
     *
     * @return true if and only if client is ready for communication
     */
    @Override
    public boolean isReady() {
        return state == ConnectionState.CONNECTED && handshakeFinished();
    }

    protected boolean handshakeFinished() {
        return channelFuture.channel().attr(CommonConstants.ATTR_HANDSHAKE_DONE).get();
    }

    /**
     * Block caller until {@link Channel} is connected and protocol is
     * negotiated with handshake.
     */
    protected void blockUntilConnected() {
        for (;;) {
            if (ConnectionState.DONE == state) {
                throw new IllegalStateException("Client connection is done");
            }
            if (isReady()) {
                return;
            } else {
                block();
            }
        }
    }

    @Override
    public void close() {
        closeChannel();
        super.close();
    }

    private void block() {
        LOG.trace("Waiting for connection to be established and negotiated...");
        Thread.yield();
        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void awaitConnection() {
        blockUntilConnected();
    }

    @Override
    public String toString() {
        return "AbstractReconnectingClient [state=" + state + ", uri=" + uri + ", sessionType=" + sessionType
                + ", hashCode=" + hashCode() + "]";
    }
}
