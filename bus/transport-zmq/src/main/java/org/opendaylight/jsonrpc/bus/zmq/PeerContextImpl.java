/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import io.netty.channel.Channel;

import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.spi.AbstractPeerContext;

/**
 * This class holds info about remote peer.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 24, 2018
 */
@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class PeerContextImpl extends AbstractPeerContext {
    private boolean isServerSocket;
    private SessionType socketType;
    private String identity;

    public PeerContextImpl(final Channel channel) {
        super(channel);
    }

    public void setServerSocket(boolean isServer) {
        this.isServerSocket = isServer;
    }

    public void setSocketType(SessionType socketType) {
        this.socketType = socketType;
    }

    public SessionType getSocketType() {
        return socketType;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public boolean isServerSocket() {
        return isServerSocket;
    }

    @Override
    public void send(String message) {
        channel.writeAndFlush(Util.serializeMessage(message));
    }

    @Override
    public String toString() {
        return "PeerContextImpl [channel=" + channel() + ", isServer=" + isServerSocket() + ", " + "socketType="
                + getSocketType() + ", identity=" + getIdentity() + "]";
    }
}
