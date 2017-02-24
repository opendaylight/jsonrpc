/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.hmap;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Strings;

/**
 * Implementation of a HierarchicalMap which uses {@link HashMap}
 * internally to perform child lookups and {@link EnumMap} for key-value
 * mapping.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class HierarchicalEnumHashMap<P, K extends Enum<K>, D, I> implements HierarchicalEnumMap<P, K, D> {
    private final EnumTreeNode<I, K, D> root;
    private final PathCodec<P, I> pathCodec;

    private HierarchicalEnumHashMap(Class<K> keyType, PathCodec<P, I> pathSupplier) {
        this.pathCodec = Objects.requireNonNull(pathSupplier);
        this.root = newRootNode(Objects.requireNonNull(keyType));
    }

    public static <P, K extends Enum<K>, D, I> HierarchicalEnumMap<P, K, D> create(Class<K> keyType,
            PathCodec<P, I> pathSupplier) {
        return new HierarchicalEnumHashMap<>(keyType, pathSupplier);
    }

    @Override
    public Optional<D> lookup(P path, K key) {
        EnumTreeNode<I, K, D> current = root;
        D value = null;
        final Iterator<I> iterator = pathCodec.serialize(path).iterator();
        if(!iterator.hasNext()) {
            return Optional.ofNullable(root.value(key));
        }
        while (iterator.hasNext()) {
            final I id = iterator.next();
            if (current.value(key) != null) {
                value = current.value(key);
            }
            final Optional<EnumTreeNode<I, K, D>> candidate = current.lookupChild(id);
            if (candidate.isPresent()) {
                current = candidate.get();
                if (current.value(key) != null) {
                    value = current.value(key);
                }
            } else {
                current = current.appendChild(id);
            }
        }
        return Optional.ofNullable(value);
    }

    @Override
    public D put(P path, K key, D data) {
        EnumTreeNode<I, K, D> current = root;
        final Iterator<I> iterator = pathCodec.serialize(path).iterator();
        while (iterator.hasNext()) {
            final I id = iterator.next();
            final Optional<EnumTreeNode<I, K, D>> candidate = current.lookupChild(id);
            current = candidate.isPresent() ? candidate.get() : current.appendChild(id);
        }
        final D previousValue = current.value(key);
        current.setValue(key, data);
        return previousValue;
    }

    @Override
    public Map<P, D> toMap(K key) {
        final Map<P, D> map = new HashMap<>();
        final LinkedList<I> currentPath = new LinkedList<>();
        collectChildren(root, key, currentPath, map);
        return Collections.unmodifiableMap(map);
    }

    private void collectChildren(EnumTreeNode<I, K, D> current, K key, LinkedList<I> currentPath, Map<P, D> map) {
        final LinkedList<I> newPath = new LinkedList<>(currentPath);
        newPath.addLast(current.id());
        final D value = current.value(key);
        if (value != null) {
            final P path = pathCodec.deserialize(newPath);
            map.put(path, value);
        }
        for (final EnumTreeNode<I, K, D> child : current.children()) {
            collectChildren(child, key, newPath, map);
        }
    }

    @Override
    public String dump() {
        final StringBuilder sb = new StringBuilder();
        sb.append("\n");
        append(sb, root, 1);
        return sb.toString();
    }

    private void append(StringBuilder sb, EnumTreeNode<I, K, D> node, int level) {
        sb.append(Strings.repeat(" ", level * 2));
        sb.append(node.id()).append("[").append(node.allValues()).append("]");
        sb.append("\n");
        for (final EnumTreeNode<I, K, D> child : node.children()) {
            append(sb, child, level + 1);
        }
    }

    private static <I, K extends Enum<K>, D> RootTreeNode<I, K, D> newRootNode(Class<K> keyType) {
        return new RootTreeNode<>(keyType);
    }

    static class ChildTreeNode<I, K extends Enum<K>, D> extends AbstractTreeNode<I, K, D> {
        public ChildTreeNode(I id, Class<K> keyType, EnumTreeNode<I, K, D> parent) {
            super(id, keyType);
        }
    }

    private static class RootTreeNode<I, K extends Enum<K>, D> extends AbstractTreeNode<I, K, D> {
        public RootTreeNode(Class<K> keyType) {
            super(null, keyType);
        }
    }

    private abstract static class AbstractTreeNode<I, K extends Enum<K>, D> implements EnumTreeNode<I, K, D> {
        private final I id;
        private final Map<I, EnumTreeNode<I, K, D>> children;
        private final EnumMap<K, D> value;
        private final Class<K> keyType;

        private AbstractTreeNode(I id, Class<K> keyType) {
            this.id = id;
            this.children = new ConcurrentHashMap<>();
            this.value = new EnumMap<>(keyType);
            this.keyType = keyType;
        }

        @Override
        public I id() {
            return id;
        }

        @Override
        public Collection<EnumTreeNode<I, K, D>> children() {
            return Collections.unmodifiableCollection(children.values());
        }

        @Override
        public Optional<EnumTreeNode<I, K, D>> lookupChild(I childId) {
            return Optional.ofNullable(children.get(childId));
        }

        @Override
        public void setValue(K key, D data) {
            this.value.put(key, data);
        }

        @Override
        public D value(K key) {
            return value.get(key);
        }

        @Override
        public Map<K, D> allValues() {
            return Collections.unmodifiableMap(value);
        }

        @Override
        public EnumTreeNode<I, K, D> appendChild(I id) {
            final EnumTreeNode<I, K, D> child = newNode(id, keyType, this);
            children.put(id, child);
            return child;
        }

        private static <I, K extends Enum<K>, D> EnumTreeNode<I, K, D> newNode(I id, Class<K> keyType,
                EnumTreeNode<I, K, D> parent) {
            return new ChildTreeNode<>(id, keyType, parent);
        }
    }
}
