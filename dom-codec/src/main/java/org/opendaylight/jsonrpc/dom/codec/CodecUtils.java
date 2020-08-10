/*
 * Copyright (c) 2020 dNation.cloud. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.dom.codec;

import com.google.common.annotations.Beta;
import com.google.gson.JsonElement;
import java.io.IOException;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.model.api.Module;

@Beta
public final class CodecUtils {
    private CodecUtils() {
        // uitility class constructor
    }

    /**
     * Create qualified name for local node. This method mainly exists to de-duplicate same code across project.
     *
     * @param module namespace
     * @param qname local name
     * @return qualified name
     */
    public static String makeQualifiedName(Module module, QName qname) {
        return new StringBuilder().append(module.getName()).append(':').append(qname.getLocalName()).toString();
    }

    /**
     * Convenient method to perform serialization of data without need to handle declared {@link IOException}.
     *
     * @param codecFactory {@link JSONCodecFactory} used to supply data {@link Codec}.
     * @param path path to data
     * @param data actual data to serialize into {@link JsonElement}
     * @return serialized data, possibly <code>null</code>
     */
    @Nullable
    public static JsonElement encodeUnchecked(JsonRpcCodecFactory codecFactory, YangInstanceIdentifier path,
            NormalizedNode<?, ?> data) {
        try {
            return codecFactory.dataCodec(path).serialize(data);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Convenient method to perform deserialization of data without need to handle declared {@link IOException}.
     *
     * @param codecFactory {@link JSONCodecFactory} used to supply data {@link Codec}.
     * @param path path to data
     * @param data actual data to deserialize into {@link NormalizedNode}
     * @return deserialized data, possibly <code>null</code>
     */
    @Nullable
    public static NormalizedNode<?, ?> decodeUnchecked(JsonRpcCodecFactory codecFactory,
            YangInstanceIdentifier path, JsonElement data) {
        try {
            return codecFactory.dataCodec(path).deserialize(data);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
