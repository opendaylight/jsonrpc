/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.security.noop;

import java.util.Optional;

import org.opendaylight.jsonrpc.security.api.AuthenticationProvider;

/**
 * NOOP implementation of {@link AuthenticationProvider}. Any given credentials
 * are considered as valid by this provider.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jun 8, 2018
 */
public final class NoopAuthenticationProvider implements AuthenticationProvider {
    public static final NoopAuthenticationProvider INSTANCE = new NoopAuthenticationProvider();

    private NoopAuthenticationProvider() {
        // prevent others to create instances of this class
    }

    @Override
    public boolean validate(String username, String password) {
        return true;
    }

    @Override
    public Optional<String[]> lookupCredentials(String username) {
        return Optional.empty();
    }
}
