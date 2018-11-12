/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.StandardCharsets;

import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.spi.CommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler which perform handshake with remote peer.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 16, 2018
 */
public class HandshakeHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger LOG = LoggerFactory.getLogger(HandshakeHandler.class);
    private HandshakeState state = HandshakeState.SIGNATURE;
    private final byte majorVersion;

    public HandshakeHandler() {
        this(Constants.CURRENT_MAJOR_VERSION);
    }

    public HandshakeHandler(byte majorVersion) {
        this.majorVersion = majorVersion;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ctx.channel().attr(CommonConstants.ATTR_HANDSHAKE_DONE).set(false);
        ctx.channel().write(Constants.DEFAULT_SINATURE);
        ctx.channel().writeAndFlush(new ByteWrapper(majorVersion));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        while (msg.isReadable() && state != HandshakeState.DONE) {
            readInternal(ctx, msg);
        }
    }

    private void readInternal(ChannelHandlerContext ctx, ByteBuf msg) {
        if (CommonConstants.DEBUG_MODE) {
            LOG.debug("IN {} : {} : {}", ctx.channel().localAddress(), state, ByteBufUtil.hexDump(msg));
        }
        final SessionType socketType = ctx.channel().attr(CommonConstants.ATTR_SOCKET_TYPE).get();
        switch (state) {
            case SIGNATURE:
                processSignature(msg);
                break;

            case VERSION_MAJOR:
                processMajorVersion(ctx, msg, socketType);
                break;

            case SOCKET_TYPE:
                processSocketType(ctx, msg, socketType);
                break;

            case IDENTITY:
                processIdentity(ctx, msg);
                break;

            case MECHANISM:
                processAuthMechanism(ctx, msg, socketType);
                break;

            case READY:
                processReadyCommand(ctx, msg, socketType);
                break;

            case DONE:
                ctx.fireChannelReadComplete();
                break;

            default:
                throw new IllegalStateException("FSM error");
        }
    }

    private void processSignature(ByteBuf msg) {
        msg.skipBytes(10);
        state = HandshakeState.VERSION_MAJOR;
    }

    private void processSocketType(ChannelHandlerContext ctx, ByteBuf msg, final SessionType socketType) {
        SessionType remoteSocketType;
        ctx.channel().attr(Constants.ATTR_REMOTE_PEER).get().setServerSocket(false);
        remoteSocketType = Constants.getSessionType(msg.readByte());
        ctx.channel().attr(Constants.ATTR_REMOTE_PEER).get().setSocketType(remoteSocketType);
        ensureSocketType(ctx, socketType, remoteSocketType);
        state = HandshakeState.IDENTITY;
    }

    private void ensureSocketType(ChannelHandlerContext ctx, final SessionType socketType,
            SessionType remoteSocketType) {
        if (!Util.assertSocketType(socketType, remoteSocketType)) {
            LOG.warn("Remote socket type '{}' is not allowed to local '{}', closing channel", remoteSocketType,
                    socketType);
            ctx.channel().close();
        }
    }

    private void processIdentity(ChannelHandlerContext ctx, ByteBuf msg) {
        msg.skipBytes(2);
        ctx.channel().attr(CommonConstants.ATTR_HANDSHAKE_DONE).set(true);
        ctx.pipeline().remove(HandshakeHandler.this);
        LOG.debug("Handshake completed with {}", ctx.channel().attr(Constants.ATTR_REMOTE_PEER).get());
        ctx.fireUserEventTriggered(Constants.HANDSHAKE_COMPLETED);
        state = HandshakeState.DONE;
        if (msg.isReadable()) {
            ctx.fireChannelRead(msg.retain());
        }
    }

    private void processReadyCommand(ChannelHandlerContext ctx, ByteBuf msg, final SessionType socketType) {
        SessionType remoteSocketType;
        final ReadyCommand ready = new ReadyCommand(msg);
        if (ready.getMetadata().containsKey(Constants.METADATA_SOCKET_TYPE)) {
            final String socketTypeStr = ready.getMetadata()
                    .get(Constants.METADATA_SOCKET_TYPE)
                    .toString(StandardCharsets.US_ASCII);
            remoteSocketType = SessionType.valueOf(socketTypeStr);
            ensureSocketType(ctx, socketType, remoteSocketType);
            ctx.channel().attr(Constants.ATTR_REMOTE_PEER).get().setSocketType(remoteSocketType);
        }
        if (ready.getMetadata().containsKey(Constants.METADATA_IDENTITY)) {
            final String identityStr = ready.getMetadata()
                    .get(Constants.METADATA_IDENTITY)
                    .toString(StandardCharsets.US_ASCII);
            ctx.channel().attr(Constants.ATTR_REMOTE_PEER).get().setIdentity(identityStr);
        }
        LOG.trace("Ready command received : {}", ready);
        state = HandshakeState.DONE;
        ctx.channel().attr(CommonConstants.ATTR_HANDSHAKE_DONE).set(true);
        ctx.pipeline().remove(this);
        LOG.debug("Handshake completed with {}", ctx.channel().attr(Constants.ATTR_REMOTE_PEER).get());
        ctx.fireUserEventTriggered(Constants.HANDSHAKE_COMPLETED);
    }

    private void processAuthMechanism(ChannelHandlerContext ctx, ByteBuf msg, final SessionType socketType) {
        // minor version residual
        msg.skipBytes(1);
        final Mechanism mech = new DefaultMechanism(msg);
        if (!Constants.NULL_AUTH.name().equals(mech.name())) {
            LOG.warn("Unsupported mechanism : {}, closing channel", mech);
            ctx.channel().close();
            ctx.fireExceptionCaught(new UnsupportedOperationException());
        }
        LOG.trace("Authentication mechanism : {}", msg);
        final ServerIndication serverInfo = new DefaultServerIndication(msg);
        ctx.channel().attr(Constants.ATTR_REMOTE_PEER).get().setServerSocket(serverInfo.isServer());
        state = HandshakeState.READY;
        ctx.channel().writeAndFlush(new ReadyCommand(socketType)).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    LOG.warn("Unable to perform handshake, closing {}", future.channel(), future.cause());
                    future.channel().close();
                }
            }
        });
    }

    private void processMajorVersion(ChannelHandlerContext ctx, ByteBuf msg, final SessionType socketType) {
        int peerMajorVersion;
        peerMajorVersion = msg.readByte();
        LOG.debug("Peer {} advertised major version {}", ctx.channel(), peerMajorVersion);
        if (peerMajorVersion >= 3) {
            // If the peer version number is 3 or higher, the peer is
            // using ZMTP 3.0, so
            // send the rest of the greeting and continue with ZMTP 3.0.
            ctx.channel().write(new ByteWrapper(Constants.CURRENT_MINOR_VERSION));
            ctx.channel().write(Constants.NULL_AUTH);
            ctx.channel().writeAndFlush(new DefaultServerIndication(false)).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    state = HandshakeState.MECHANISM;
                }
            });
        } else {
            // If the peer version number is 1 or 2, the peer is using
            // ZMTP 2.0, so send the
            // ZMTP 2.0 socket type and identity and continue with ZMTP
            // 2.0.
            state = HandshakeState.SOCKET_TYPE;
            ctx.channel().write(new ByteWrapper(Constants.getZmtp20Socket(socketType)));
            ctx.channel().write(new ByteWrapper((byte) 0));
            ctx.channel().write(new ByteWrapper((byte) 0));
            ctx.channel().flush();
        }
    }
}
