/*
 * Copyright (c) 2020 dNation.cloud. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.dom.codec;

import com.google.gson.JsonElement;
import java.io.IOException;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

/**
 * Combination of input and output codec for RPCs.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Oct 12, 2020
 */
final class RpcIoCodecHolder {
    private final Codec<JsonElement, ContainerNode, IOException> output;
    private final Codec<JsonElement, ContainerNode, IOException> input;

    RpcIoCodecHolder(EffectiveModelContext context, RpcDefinition definition) {
        output = RpcCodec.create(context, definition, "out", definition::getOutput);
        input = RpcCodec.create(context, definition, "in", definition::getInput);
    }

    public Codec<JsonElement, ContainerNode, IOException> output() {
        return output;
    }

    public Codec<JsonElement, ContainerNode, IOException> input() {
        return input;
    }
}