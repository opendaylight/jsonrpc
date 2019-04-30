/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 * Copyright (c) 2018 Cambridge Greys, Ltd. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcException;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcNotificationMessage;
import org.opendaylight.jsonrpc.model.JSONRPCArg;
import org.opendaylight.jsonrpc.model.JsonRpcNotification;
import org.opendaylight.jsonrpc.model.NotificationContainerProxy;
import org.opendaylight.jsonrpc.model.NotificationState;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
/* rpc special casing */
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonWriterFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class JsonConverter converts YangInstanceIdentifier paths and
 * NormalizedNode data into json objects according to <a href="https://tools.ietf.org/html/rfc7951">RFC7951</a>.
 *
 * <p>
 * 1. Namespace is stripped from data, paths retain namespace (no point to ship in both)
 * 2. Objects are adjusted for BUS to ODL semantics and vice versa.
 * ODL has the expectation that the data overlays with the path by one
 * element. That is not how the bus operates - it has no overlap - the
 * path points to where the data should go and the data itself does not
 * carry an enclosing element. Example
 *          PATH                         DATA
 * ODL      {"a":{"b":{}}}      {"b":{"b1":"b1-value", "b2":"b2-value"}}
 * BUS      {"a":{"b":{}}}      {"b1":"b1-value", "b2":"b2-value"}
 *
 * <p>
 * This is not particularly efficient as it relies on ODL json reader/writers
 * It will benefit significantly from converting these to a custom gson
 * serializer/deserializer and registering it with gson
 *
 */
public class JsonConverter {
    private static final char COLON = ':';
    private static final Logger LOG = LoggerFactory.getLogger(JsonConverter.class);
    private static final JSONRPCArg EMPTY_RPC_ARG = new JSONRPCArg(null, null);
    private static final JsonParser PARSER = new JsonParser();
    private final SchemaContext schemaContext;

    /**
     * Instantiates a new json converter.
     *
     * @param schemaContext the schema context
     */
    public JsonConverter(SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
    }

    /**
     * Convert {@link JsonRpcNotificationMessage} into {@link DOMNotification}.
     *
     * @param notification inbound bus notification
     * @param mappedNotifications peer's mapped notifications
     * @return {@link DOMNotification} instance.
     */
    public DOMNotification notificationConvert(final JsonRpcNotificationMessage notification,
            final Map<String, NotificationState> mappedNotifications) {
        final String method = notification.getMethod();
        final JsonElement parsed;
        final NotificationState ns;

        LOG.debug("Got notification {}", notification);
        if (!mappedNotifications.containsKey(method)) {
            LOG.error("Notification not mapped {}", method);
            return null;
        }
        ns = mappedNotifications.get(method);
        try {
            parsed = notification.getParamsAsObject(JsonElement.class);
        } catch (JsonRpcException e) {
            LOG.error("Error processing notification", e);
            return null;
        }

        final JsonObject digested;
        if (parsed.isJsonObject()) {
            digested = parsed.getAsJsonObject();
        } else if (parsed.isJsonArray()) {
            digested = new JsonObject();
            final List<DataSchemaNode> childNodes = new ArrayList<>(ns.notification().getChildNodes());
            for (int i = 0; i < childNodes.size() && i < parsed.getAsJsonArray().size(); i++) {
                digested.add(childNodes.get(i).getQName().getLocalName(), parsed.getAsJsonArray().get(i));
            }
        } else {
            digested = new JsonObject();
            LOG.warn("Invalid JSON in payload will be ignored : {}", parsed);
        }

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> builder = ImmutableContainerNodeBuilder
                .create().withNodeIdentifier(NodeIdentifier.create(ns.notification().getQName()));
        final DOMNotification deserialized = extractNotification(ns, digested, builder);
        LOG.debug("Deserialized {}", deserialized);
        return deserialized;
    }

    private DOMNotification extractNotification(NotificationState notificationState, JsonElement jsonResult,
            DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> notificationBuilder) {
        final Date eventTime = new Date();
        try (NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(notificationBuilder);
                JsonParserStream jsonParser = JsonParserStream.create(streamWriter,
                        JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.getShared(schemaContext),
                        new NotificationContainerProxy(notificationState.notification()))) {
            jsonParser.parse(new JsonReader(new StringReader(jsonResult.toString())));
            return new JsonRpcNotification(notificationBuilder.build(), eventTime,
                    notificationState.notification().getPath());
        } catch (IOException e1) {
            LOG.error("Failed to close JSON parser", e1);
            return null;
        }
    }

    /**
     * Performs the data conversion for an RPC argument.
     *
     * @param path - Schema path for the rpc data.
     * @param data - Normalized Node argument as passed to the rpc
     * @return data argument converted to JsonObject as expected by RPC calls
     */
    public JsonObject rpcConvert(SchemaPath path, ContainerNode data) {
        LOG.debug("Converting node {} at path {}", data, path);
        final StringWriter writer = new StringWriter();
        final JsonWriter jsonWriter = JsonWriterFactory.createJsonWriter(writer);
        final NormalizedNodeStreamWriter streamWriter = JSONNormalizedNodeStreamWriter.createNestedWriter(
                JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.createSimple(schemaContext), path, null,
                jsonWriter);
        final NormalizedNodeWriter nodeWriter = NormalizedNodeWriter.forStreamWriter(streamWriter);
        try {
            jsonWriter.beginObject();
            for (final DataContainerChild<? extends PathArgument, ?> child : data.getValue()) {
                nodeWriter.write(child);
            }
            jsonWriter.endObject();
            jsonWriter.flush();
            JsonObject dataWithModule = null;
            dataWithModule = PARSER.parse(writer.toString()).getAsJsonObject();
            JsonObject newData = new JsonObject();

            /*
             * The data is generated with a module prefix - this does not match our implementation
             * semantics, we have to strip it, otherwise the bus side does not like it.
             **/
            for (final Entry<String, JsonElement> element : dataWithModule.entrySet()) {
                final String property = element.getKey();
                final int idx = element.getKey().indexOf(COLON);
                if (idx != -1) {
                    newData.add(property.substring(idx + 1),element.getValue());
                } else {
                    newData.add(property, element.getValue());
                }
            }
            return newData;
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /**
     * Performs the actual data conversion.
     *
     * @param qnames - a list of qnames pointing to the schema root
     * @param data - Normalized Node
     * @return converted data argument as a JsonObject
     */

    private JsonObject doConvert(List<QName> qnames, NormalizedNode<?, ?> data) {
        if (!qnames.isEmpty()) {
            return doConvert(SchemaPath.create(qnames, true), data);
        } else {
            return doConvert(SchemaPath.ROOT, data);
        }
    }

    /**
     * Performs the actual data conversion.
     *
     * @param schemaPath - schema path for data
     * @param data - Normalized Node
     * @return data converted as a JsonObject
     */
    private JsonObject doConvert(SchemaPath schemaPath, NormalizedNode<?, ?> data) {
        final StringWriter writer = new StringWriter();
        final JsonWriter jsonWriter = JsonWriterFactory.createJsonWriter(writer);
        final JSONCodecFactory codecFactory = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02
                .createSimple(schemaContext);
        final NormalizedNodeStreamWriter jsonStream;
        if (data instanceof MapEntryNode) {
            jsonStream = JSONNormalizedNodeStreamWriter.createNestedWriter(
                    codecFactory,
                    schemaPath,
                    null,
                    jsonWriter
                );
        } else {
            jsonStream = JSONNormalizedNodeStreamWriter.createExclusiveWriter(
                    codecFactory,
                   schemaPath,
                    null,
                    jsonWriter
                );
        }
        final NormalizedNodeWriter nodeWriter = NormalizedNodeWriter.forStreamWriter(jsonStream);
        try {
            nodeWriter.write(data);
            nodeWriter.flush();
            String jsonValue = writer.toString();
            if (!jsonValue.startsWith("{")) {
                jsonValue = "{" + jsonValue + "}";
            }
            if (data instanceof LeafNode) {
                jsonValue += '}';
            }
            return PARSER.parse(jsonValue).getAsJsonObject();
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /**
     * Create qualified name for local node. This method mainly exists to
     * de-duplicate same code across project.
     *
     * @param module namespace
     * @param qname local name
     * @return qualified name
     */
    public String makeQualifiedName(Module module, QName qname) {
        return new StringBuilder()
                .append(module.getName())
                .append(COLON)
                .append(qname.getLocalName())
                .toString();
    }

    /**
     * Convert a yang instance identifier path and (optionally) data to netmod draft-json format.
     *
     * @param path the YangInstanceIdentifier path
     * @param data the Data
     * @param strip to control outer most element stripping
     * @return an object containing a converted path and data
     *
     */
    public JSONRPCArg toBusWithStripControl(YangInstanceIdentifier path, NormalizedNode<?, ?> data, boolean strip) {
        Iterator<PathArgument> pathIterator = path.getPathArguments().iterator();
        List<QName> qnames = new ArrayList<>();
        boolean topLevel = true;
        JsonObject pathJson;
        JsonElement dataJson = null;
        JsonObject previous;
        JsonObject tracker;
        String lastKey;
        String rootKey;
        String activeModule;

        if (pathIterator.hasNext()) {
            pathJson = new JsonObject();
            tracker = new JsonObject();
            PathArgument root = pathIterator.next();
            QName nodeType = root.getNodeType();
            if (pathIterator.hasNext()) {
                qnames.add(nodeType);
            }


            QNameModule qmodule = nodeType.getModule();

            Optional<Module> possibleModule = schemaContext.findModule(qmodule.getNamespace(), qmodule.getRevision());
            Preconditions.checkState(possibleModule.isPresent(),
                    "Could not find module for namespace %s and revision %s", qmodule.getNamespace(),
                    qmodule.getRevision());

            activeModule = possibleModule.get().getName();
            rootKey = makeQualifiedName(possibleModule.get(), root.getNodeType());
            lastKey = rootKey;
            pathJson.add(rootKey, tracker);
        } else {
            return EMPTY_RPC_ARG;
        }

        previous = tracker;

        while (pathIterator.hasNext()) {
            PathArgument pathArg = pathIterator.next();
            JsonObject nextLevel = new JsonObject();
            if (pathArg instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates) {

                Map<QName, Object> keyValues = ((YangInstanceIdentifier.NodeIdentifierWithPredicates) pathArg)
                        .getKeyValues();

                for (Entry<QName, Object> entry : keyValues.entrySet()) {
                    nextLevel.add(entry.getKey().getLocalName(), new JsonPrimitive(entry.getValue().toString()));
                }

                JsonArray jsonArray = new JsonArray();
                jsonArray.add(nextLevel);

                lastKey = pathArg.getNodeType().getLocalName();
                if (topLevel) {
                    pathJson.remove(rootKey);
                    pathJson.add(rootKey, jsonArray);
                } else {
                    previous.remove(lastKey);
                    previous.add(lastKey, jsonArray);
                }
            } else {
                if (pathIterator.hasNext()) {
                    qnames.add(pathArg.getNodeType());
                }
                lastKey = pathArg.getNodeType().getLocalName();
                tracker.add(lastKey, nextLevel);
            }
            previous = tracker;
            tracker = nextLevel;
            topLevel = false;
        }

        if (data != null) {
            dataJson = toBusData(data, strip, qnames, lastKey, activeModule);
        }
        return new JSONRPCArg(pathJson, dataJson);
    }


    /*
     * KLUDGE - TODO we reuse the ODL ser/des which emits string to
     * stream we should have this replaced by something that generates
     * JSON natively
     */
    private JsonElement toBusData(NormalizedNode<?, ?> data, boolean strip, List<QName> qnames, String lastKey,
            String activeModule) {
        JsonElement dataJson;
        JsonObject newData = doConvert(qnames, data);
        if (newData == null) {
            dataJson = null;
        } else {
            if (!newData.has(lastKey)) {
                if (!newData.has(activeModule + COLON + lastKey)) {
                    dataJson = newData;
                } else {
                    dataJson = decideStrip(newData, activeModule + COLON + lastKey, strip);
                }
            } else {
                dataJson = decideStrip(newData, lastKey, strip);
            }
        }
        return dataJson;
    }

    private JsonElement decideStrip(JsonObject data, String outerElement, boolean strip) {
        return strip ? data.get(outerElement) : data;
    }

    /**
     * Convert {@link NormalizedNode} at given {@link YangInstanceIdentifier}
     * path into JSON payload expected by bus.
     *
     * @param path the {@link YangInstanceIdentifier} path
     * @param data data to convert
     * @return an object containing a converted path and data
     */
    public JSONRPCArg toBus(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        return toBusWithStripControl(path, data, true);
    }

    /**
     * Convert bus received data to the ODL expected form.
     *
     * @param path the YangInstanceIdentifier path
     * @param jsonElement to prepend
     * @return the prepended json string
     */
    public JsonObject fromBus(final YangInstanceIdentifier path, JsonElement jsonElement) {
        Iterator<PathArgument> it = path.getPathArguments().iterator();
        if (!it.hasNext()) {
            return null;
        }

        /* Pull the last "key" off the path */

        PathArgument pathArg = it.next();

        QName nodeType = pathArg.getNodeType();

        QNameModule qmodule = nodeType.getModule();

        Optional<Module> possibleModule = schemaContext.findModule(
            qmodule.getNamespace(),
            qmodule.getRevision());
        Preconditions.checkState(possibleModule.isPresent(),
            "Could not find module for namespace %s and revision %s",
            qmodule.getNamespace(),
            qmodule.getRevision()
        );


        /* use last here */
        while (it.hasNext()) {
            pathArg = it.next();
        }
        final String rootKey = makeQualifiedName(possibleModule.get(), pathArg.getNodeType());

        JsonObject result = new JsonObject();
        if (pathArg instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates) {
            JsonArray asArray = new JsonArray();
            if (jsonElement != null && !jsonElement.isJsonNull()) {
                asArray.add(jsonElement);
            }
            result.add(rootKey, asArray);
        } else {
            if (jsonElement != null && !jsonElement.isJsonNull()) {
                result.add(rootKey, jsonElement);
            } else {
                /* special case - read of empty container contents */
                result.add(rootKey, new JsonObject());
            }
        }
        return result;
    }

    private SchemaNode findParentSchema(YangInstanceIdentifier yii) {
        final Optional<DataSchemaContextNode<?>> child = DataSchemaContextTree.from(schemaContext).findChild(yii);
        SchemaNode parentSchema;
        if (SchemaPath.ROOT.equals(child.get().getDataSchemaNode().getPath().getParent())) {
            parentSchema = schemaContext;
        } else {
            parentSchema = SchemaContextUtil.findDataSchemaNode(schemaContext,
                    child.get().getDataSchemaNode().getPath().getParent());
        }
        return parentSchema;
    }

    /**
     * Convert JSON representation of data into {@link NormalizedNode} based on
     * given {@link YangInstanceIdentifier} path.
     *
     * @param data JSON data
     * @param path path in schema tree
     * @return {@link NormalizedNode}
     */
    public NormalizedNode<?, ?> jsonElementToNormalizedNode(JsonElement data, YangInstanceIdentifier path) {
        return jsonElementToNormalizedNode(data, path, false);
    }

    /**
     * Convert JSON representation of data into {@link NormalizedNode} based on given {@link YangInstanceIdentifier}
     * path. Optionally, JSON can be reduced to minimalistic form, where wrap flag is set to true
     *
     * @param data JSON data
     * @param path path in schema tree
     * @param wrap flag to indicate that JSON data are in reduced form
     * @return {@link NormalizedNode}
     */
    public NormalizedNode<?, ?> jsonElementToNormalizedNode(JsonElement data, YangInstanceIdentifier path,
            boolean wrap) {
        final NormalizedNodeResult resultHolder = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(resultHolder);
        final SchemaNode parentSchema = findParentSchema(path);
        try (JsonParserStream jsonParser = JsonParserStream.create(writer,
                JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.getShared(schemaContext), parentSchema)) {
            final JsonReader reader = new JsonReader(
                    new StringReader((wrap ? wrapReducedJson(path, data) : data).toString()));
            jsonParser.parse(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to close JsonParserStream", e);
        }
        NormalizedNode<?, ?> result = resultHolder.getResult();
        if (result instanceof MapNode) {
            result = Iterables.getOnlyElement(((MapNode) result).getValue());
        }
        LOG.debug("Parsed result : {}", result);
        return result;
    }

    /**
     * To support simplified JSON data, we need to wrap it.
     */
    private JsonElement wrapReducedJson(YangInstanceIdentifier id, JsonElement data) {
        final QName qname = id.getLastPathArgument().getNodeType();
        final JsonObject wrapper = new JsonObject();
        final String elName = qname.getLocalName();
        final JsonArray arr = new JsonArray();
        arr.add(data);
        wrapper.add(elName, arr);
        return wrapper;
    }
}
