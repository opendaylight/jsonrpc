/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.security.noop;

import org.opendaylight.jsonrpc.security.api.AuthenticationProvider;
import org.opendaylight.jsonrpc.security.api.SecurityService;

/**
 * NOOP implementation of {@link SecurityService}. Used when no security features are needed.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jun 8, 2018
 */
public final class NoopSecurityService implements SecurityService {
    public static final NoopSecurityService INSTANCE = new NoopSecurityService();

    private NoopSecurityService() {
        // prevent others to create instances of this class
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return NoopAuthenticationProvider.INSTANCE;
    }
}
