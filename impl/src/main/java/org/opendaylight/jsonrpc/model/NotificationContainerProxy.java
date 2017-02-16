/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.ConstraintDefinition;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UsesNode;

import com.google.common.collect.Sets;

/**
 * Simple proxy for container like schema nodes, where user provides a
 * collection of children schema nodes
 */
public final class NotificationContainerProxy implements ContainerSchemaNode {

    private final Set<AugmentationSchema> availableAugmentations;
    private final Map<QName, DataSchemaNode> childNodes = new HashMap<>();
    private final QName qName;

    public NotificationContainerProxy(final NotificationDefinition def) {
        this.availableAugmentations = def.getAvailableAugmentations();
        for (DataSchemaNode element : def.getChildNodes()) {
            this.childNodes.put(element.getQName(), element);
        }
        this.qName = def.getQName();
    }

    @Override
    public Set<TypeDefinition<?>> getTypeDefinitions() {
        return Collections.emptySet();
    }

    @Override
    public Set<DataSchemaNode> getChildNodes() {
        return Sets.newHashSet(childNodes.values());
    }

    @Override
    public Set<GroupingDefinition> getGroupings() {
        return Collections.emptySet();
    }

    @Override
    public DataSchemaNode getDataChildByName(final QName qName) {
        return childNodes.get(qName);
    }

    @Override
    public Set<UsesNode> getUses() {
        return Collections.emptySet();
    }

    @Override
    public boolean isPresenceContainer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<AugmentationSchema> getAvailableAugmentations() {
        return availableAugmentations;
    }

    @Override
    public boolean isAugmenting() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAddedByUses() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConfiguration() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintDefinition getConstraints() {
        throw new UnsupportedOperationException();
    }

    @Override
    public QName getQName() {
        return qName;
    }

    @Override
    public SchemaPath getPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getReference() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Status getStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        return Collections.emptyList();
    }
}
