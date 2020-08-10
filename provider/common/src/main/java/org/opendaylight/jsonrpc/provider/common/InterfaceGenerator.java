/*
 * Copyright (c) 2020 dNation.tech. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import com.google.gson.JsonObject;
import java.lang.reflect.Type;
import java.util.Objects;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy.SuffixingRandom;
import net.bytebuddy.NamingStrategy.SuffixingRandom.BaseNameResolver.ForFixedValue;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ParameterDefinition.Initial;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.api.RpcMethod;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

/**
 * Utility class used to generate proxy stub API for remote service.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Oct 12, 2020
 */
final class InterfaceGenerator {
    private static final ByteBuddy BB = new ByteBuddy(ClassFileVersion.JAVA_V8)
            .with(new SuffixingRandom("SyntheticProxy", new ForFixedValue("org.opendaylight.jsonrpc")));

    private InterfaceGenerator() {
        // utility class
    }

    /**
     * Generate RPC interface for proxy using {@link RpcDefinition}.
     *
     * @param node {@link RpcDefinition}
     * @return generated proxy interface
     */
    static Class<? extends AutoCloseable> generate(@NonNull RpcDefinition node) {
        Objects.requireNonNull(node);
        Builder<AutoCloseable> builder = BB.makeInterface(AutoCloseable.class);
        builder = generateMethod(node, builder);
        return builder.make().load(InterfaceGenerator.class.getClassLoader()).getLoaded();
    }

    private static Type getReturnType(RpcDefinition node) {
        return node.getOutput().getChildNodes().isEmpty() ? void.class : JsonObject.class;
    }

    private static boolean hasArgument(RpcDefinition node) {
        return !node.getInput().getChildNodes().isEmpty();
    }

    private static String methodName(SchemaNode node) {
        return node.getQName().getLocalName().replaceAll("-", "_");
    }

    private static Builder<AutoCloseable> generateMethod(RpcDefinition node, Builder<AutoCloseable> builder) {
        final Initial<AutoCloseable> method = builder.defineMethod(methodName(node), getReturnType(node),
                Visibility.PUBLIC);

        final AnnotationDescription annotation = AnnotationDescription.Builder.ofType(RpcMethod.class)
                .define("value", node.getQName().getLocalName())
                .build();

        if (hasArgument(node)) {
            return method.withParameter(JsonObject.class, "input").withoutCode().annotateMethod(annotation);
        } else {
            return method.withoutCode().annotateMethod(annotation);
        }
    }
}
