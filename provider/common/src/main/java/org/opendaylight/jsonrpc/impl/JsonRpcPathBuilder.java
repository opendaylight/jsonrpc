/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.opendaylight.yangtools.concepts.Builder;

/**
 * Fluent {@link Builder} of JSONRPC path.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 16, 2020
 */
@Deprecated(forRemoval = true)
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
public final class JsonRpcPathBuilder extends org.opendaylight.jsonrpc.dom.codec.JsonRpcPathBuilder {

    public static org.opendaylight.jsonrpc.dom.codec.JsonRpcPathBuilder newBuilder(String container) {
        return org.opendaylight.jsonrpc.dom.codec.JsonRpcPathBuilder.newBuilder(container);
    }

    public static org.opendaylight.jsonrpc.dom.codec.JsonRpcPathBuilder newBuilder() {
        return org.opendaylight.jsonrpc.dom.codec.JsonRpcPathBuilder.newBuilder();
    }
}
