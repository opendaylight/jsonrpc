/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import java.io.Reader;
import java.io.StringReader;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Helper class to eliminate repeated <code>new JsonReader(new StringReader(json.toString()))</code>.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 22, 2020
 * @deprecated use {@link org.opendaylight.jsonrpc.dom.codec.JsonReaderAdapter}.
 */
@Deprecated(forRemoval = true)
public final class JsonReaderAdapter extends JsonReader {
    private JsonReaderAdapter(Reader in) {
        super(in);
    }

    /**
     * Create new instance of {@link JsonReader} using provided {@link JsonElement}.
     *
     * @param json {@link JsonElement} to create reader from.
     * @return new {@link JsonReader} instance.
     */
    public static JsonReader from(@NonNull JsonElement json) {
        return new JsonReaderAdapter(new StringReader(json.toString()));
    }
}
