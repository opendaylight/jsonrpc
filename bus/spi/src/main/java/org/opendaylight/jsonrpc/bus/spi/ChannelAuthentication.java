/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import java.util.Map;
import org.opendaylight.jsonrpc.security.api.SecurityConstants;

public final class ChannelAuthentication {
    private final boolean enabled;
    private final String username;
    private final String password;

    public static ChannelAuthentication create(Map<String, String> opts) {
        return new ChannelAuthentication(opts.containsKey(SecurityConstants.OPT_REQ_AUTH),
                opts.get(SecurityConstants.OPT_USERNAME), opts.get(SecurityConstants.OPT_PASSWORD));
    }

    private ChannelAuthentication(boolean enabled, String username, String password) {
        this.enabled = enabled;
        this.username = username;
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "ChannelAuthentication [enabled=" + enabled + ", username=" + username + ", password=*******]";
    }
}
