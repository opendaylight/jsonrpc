/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

/**
 * This DTO hold negotiated SSL session parameters.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 18, 2019
 */
public class SslSessionInfo {
    private final String protocol;
    private final String cipher;

    public SslSessionInfo(String protocol, String cipher) {
        this.protocol = protocol;
        this.cipher = cipher;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getCipher() {
        return cipher;
    }

    @Override
    public String toString() {
        return "SslSessionInfo [protocol=" + protocol + ", cipher=" + cipher + "]";
    }
}
