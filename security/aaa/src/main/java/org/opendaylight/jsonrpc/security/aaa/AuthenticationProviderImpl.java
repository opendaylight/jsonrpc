/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.security.aaa;

import java.util.Objects;
import java.util.Optional;
import org.opendaylight.aaa.api.AuthenticationException;
import org.opendaylight.aaa.api.CredentialAuth;
import org.opendaylight.aaa.api.IDMStoreException;
import org.opendaylight.aaa.api.IIDMStore;
import org.opendaylight.aaa.api.PasswordCredentials;
import org.opendaylight.jsonrpc.security.api.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link AuthenticationProvider} which uses AAA-provided credential service.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since May 24, 2018
 */
public class AuthenticationProviderImpl implements AuthenticationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationProviderImpl.class);
    private final CredentialAuth<PasswordCredentials> credentialAuth;
    private final IIDMStore iidmStore;

    public AuthenticationProviderImpl(CredentialAuth<PasswordCredentials> credService, IIDMStore iidmStore) {
        this.credentialAuth = Objects.requireNonNull(credService);
        this.iidmStore = Objects.requireNonNull(iidmStore);
    }

    @Override
    public Optional<String[]> lookupCredentials(String username) {
        Objects.requireNonNull(username);
        try {
            return iidmStore.getUsers()
                    .getUsers()
                    .stream()
                    .filter(u -> username.equals(u.getName()))
                    .map(u -> new String[] { u.getName(), u.getPassword() })
                    .findFirst();
        } catch (IDMStoreException e) {
            LOG.error("Unable to lookup credentials for user '{}'", username, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean validate(String username, String password) {
        try {
            LOG.debug("Validating credentials for user {}", username);
            credentialAuth.authenticate(new PasswordCredentialsContainer(username, password));
            return true;
        } catch (AuthenticationException e) {
            LOG.warn("Authentication of user '{}' failed", username, e);
            return false;
        }
    }
}
