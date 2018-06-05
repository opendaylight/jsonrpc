/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class to deal with parsing URI's query parameters.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Apr 26, 2019
 */
public final class UriParser {
    private UriParser() {
        // utility class constructor
    }

    /**
     * Parse URI's query parameters as map of key-value pairs.
     *
     * @param endpoint raw URI to parse
     * @return {@link Map} of key-value pairs
     */
    public static Map<String, String> parse(String endpoint) {
        try {
            final URI uri = new URI(endpoint);
            return Arrays.asList(Optional.ofNullable(uri.getQuery()).orElse("").split("&")).stream().flatMap(kv -> {
                final String[] parts = kv.split("=");
                return Stream.of(new AbstractMap.SimpleImmutableEntry<>(parts[0], parts.length > 1 ? parts[1] : ""));
            }).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
