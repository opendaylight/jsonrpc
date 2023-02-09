/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.security.impl;

import java.util.Objects;
import org.opendaylight.jsonrpc.security.api.AuthenticationProvider;
import org.opendaylight.jsonrpc.security.api.SecurityService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Default implementation of {@link SecurityService}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jun 10, 2018
 */
@Component(property = "type=default")
public final class SecurityServiceImpl implements SecurityService {
    private final AuthenticationProvider authenticationProvider;

    @Activate
    public SecurityServiceImpl(@Reference(target = "(type=aaa)") final AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = Objects.requireNonNull(authenticationProvider);
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }
}
