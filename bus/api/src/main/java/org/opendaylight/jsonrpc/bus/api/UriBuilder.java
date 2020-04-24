/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.api;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper to build URI query string.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 18, 2019
 */
public class UriBuilder {
    private static final Escaper ESCAPER = UrlEscapers.urlFormParameterEscaper();
    private final String base;
    private final Map<String, String> params = new LinkedHashMap<>();
    private static final MapJoiner PARAM_JOINER = Joiner.on('&').withKeyValueSeparator('=');

    public UriBuilder(String base) {
        this.base = base;
    }

    /**
     * Add key-value pair to this URI.
     *
     * @param name key
     * @param value value
     * @return this instance
     */
    public UriBuilder add(String name, String value) {
        params.put(ESCAPER.escape(name), ESCAPER.escape(value));
        return this;
    }

    /**
     * Add/Copy all key-value mapping from other map into this instance of {@link UriBuilder}.
     *
     * @param other Map which is source of key-value pairs to be copied
     * @return this instance
     */
    public UriBuilder addAll(Map<String, String> other) {
        params.putAll(other);
        return this;
    }

    /**
     * Serialize this instance {@link UriBuilder} into URI.
     *
     * @return serialized URI
     */
    public String build() {
        final StringBuilder sb = new StringBuilder();
        sb.append(base);
        sb.append('?');
        sb.append(PARAM_JOINER.join(params));
        return sb.toString();
    }
}
