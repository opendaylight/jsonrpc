/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.security.api;

import java.util.Optional;

/**
 * Authentication provider to validate credentials.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since May 24, 2018
 */
public interface AuthenticationProvider {
    /**
     * Validate given credentials.
     *
     * @param username username
     * @param password password
     * @return true if credentials are valid, false otherwise
     */
    boolean validate(String username, String password);

    /**
     * Attempts to lookup credentials for given username.
     *
     * @param username username to lookup credentials for
     * @return {@link Optional} of 2-items {@link String} array where first
     *         element is username and second is password.
     */
    Optional<String[]> lookupCredentials(String username);
}
