/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.security.aaa;

import org.opendaylight.aaa.api.PasswordCredentials;

/**
 * Container for {@link PasswordCredentials}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since May 24, 2018
 */
public class PasswordCredentialsContainer implements PasswordCredentials {
    private final String username;
    private final String password;

    PasswordCredentialsContainer(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String username() {
        return username;
    }

    @Override
    public String password() {
        return password;
    }

    @Override
    public String domain() {
        return null;
    }
}
