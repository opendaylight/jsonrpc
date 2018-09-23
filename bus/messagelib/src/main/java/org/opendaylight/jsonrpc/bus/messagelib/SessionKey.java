/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import java.util.Objects;

import org.opendaylight.jsonrpc.bus.api.SessionType;

/**
 * SessionKey used for lookup in cache.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 22, 2018
 */
class SessionKey {
    public static final Object NOOP_HANDLER = new Object();
    private final SessionType sessionType;
    private final String uri;
    private final Object handler;

    SessionKey(final SessionType sessionType, final String uri, final Object handler) {
        this.sessionType = sessionType;
        this.uri = uri;
        this.handler = handler;
    }

    public SessionType type() {
        return sessionType;
    }

    public String uri() {
        return uri;
    }

    public Object handler() {
        return handler;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionType, uri, handler);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SessionKey other = (SessionKey) obj;
        return Objects.equals(sessionType, other.sessionType) && Objects.equals(this.uri, other.uri)
                && Objects.equals(handler, other.handler);
    }
}
