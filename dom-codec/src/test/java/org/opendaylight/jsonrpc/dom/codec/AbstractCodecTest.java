/*
 * Copyright (c) 2020 dNation.cloud. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.dom.codec;

import com.google.common.base.Strings;
import com.google.common.io.Resources;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Collection;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingCodecContext;
import org.opendaylight.mdsal.binding.runtime.spi.BindingRuntimeHelpers;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCodecTest extends AbstractDataBrokerTest {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractCodecTest.class);
    protected JsonRpcCodecFactory factory;
    protected static final BindingCodecContext BCC = new BindingCodecContext(
            BindingRuntimeHelpers.createRuntimeContext());

    @Rule
    public TestName nameRule = new TestName();
    protected EffectiveModelContext schemaContext;

    @Override
    protected void setupWithSchema(final EffectiveModelContext context) {
        schemaContext = context;
        super.setupWithSchema(context);
        factory = new JsonRpcCodecFactory(context);
    }

    protected JsonElement loadJsonData(String name) throws IOException {
        try (InputStream is = Resources.getResource(name).openStream()) {
            return JsonParser.parseReader(new InputStreamReader(is));
        }
    }

    protected NormalizedNode loadDomData(String name, YangInstanceIdentifier path) throws IOException {
        final SchemaInferenceStack stack = DataSchemaContextTree.from(schemaContext)
                .enterPath(path)
                .orElseThrow()
                .stack();
        if (!stack.isEmpty()) {
            stack.exit();
        }

        final NormalizationResultHolder resultHolder = new NormalizationResultHolder();
        final NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(resultHolder);
        try (JsonParserStream jsonParser = JsonParserStream.create(writer,
                JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.getShared(schemaContext),
                stack.toInference())) {
            jsonParser.parse(JsonReaderAdapter.from(loadJsonData(name)));
            return resultHolder.getResult().data();
        }
    }

    protected static void dumpYangPath(YangInstanceIdentifier path) {
        final StringBuilder sb = new StringBuilder();
        int level = 2;
        for (PathArgument arg : path.getPathArguments()) {
            dumpYangPathArgument(level++, sb, arg);
        }
        LOG.info("YangInstanceIdentifier : \n{}", sb.toString());
    }

    protected static void dumpYangPathArgument(int level, StringBuilder sb, PathArgument arg) {
        sb.append(Strings.repeat(" ", (level - 1) * 2));
        sb.append(" ").append(arg.getClass().getSimpleName()).append(arg).append('\n');
        if (arg instanceof NodeIdentifierWithPredicates) {
            ((NodeIdentifierWithPredicates) arg).entrySet().forEach(pre -> {
                sb.append(Strings.repeat(" ", (level - 1) * 2));
                sb.append(" key")
                        .append(':')
                        .append(pre.getKey().getLocalName())
                        .append(" => ")
                        .append(pre.getValue())
                        .append('\n');
            });
        }

    }

    protected static void dumpNormalizedNode(NormalizedNode node) {
        StringWriter sw = new StringWriter();
        dumpNormalizedNode(node, sw, 1);
        LOG.info("Normalized node content : \n{}", sw.toString());
    }

    protected static void dumpNormalizedNode(NormalizedNode nn, StringWriter sw, int level) {
        sw.write(Strings.repeat(" ", (level - 1) * 2));
        sw.write(nn.body().getClass().getSimpleName());
        sw.write(" : ");
        sw.write(nn.getIdentifier().toString());
        sw.write("\n");
        if (nn.body() instanceof Collection) {
            for (Object e : (Collection<?>) nn.body()) {
                dumpNormalizedNode((NormalizedNode) e, sw, level + 1);
            }
        }
    }
}
