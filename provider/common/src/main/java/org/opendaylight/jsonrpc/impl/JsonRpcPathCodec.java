/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * {@link Codec} to perform transformation between {@link YangInstanceIdentifier} and JSONRPC path. Instance of this
 * codec is safe to use from multiple threads concurrently.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @see org.opendaylight.jsonrpc.dom.codec.JsonRpcPathCodec
 * @since Feb 15, 2020
 */
@Deprecated(forRemoval = true)
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
public final class JsonRpcPathCodec extends org.opendaylight.jsonrpc.dom.codec.JsonRpcPathCodec {
    private JsonRpcPathCodec(SchemaContext schemaContext) {
        super(schemaContext);
    }

    public static JsonRpcPathCodec create(@NonNull SchemaContext schemaContext) {
        return new JsonRpcPathCodec(schemaContext);
    }
}
