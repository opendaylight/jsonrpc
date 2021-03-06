/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.security.api;

/**
 * {@link SecurityService} provide access to authentication.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jun 8, 2018
 */
public interface SecurityService {
    /**
     * Get Authentication provider.
     *
     * @return {@link AuthenticationProvider}
     */
    AuthenticationProvider getAuthenticationProvider();
}
