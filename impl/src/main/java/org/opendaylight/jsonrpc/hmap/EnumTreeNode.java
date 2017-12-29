/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.hmap;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * This structure represent node in data tree. There is only one top-level node
 * in tree, with null parent. Other nodes in tree has non-null parent. Data can
 * be associated with this node using enumerated keys.
 *
 *
 * @param <I> internal node identifier
 * @param <D> type of data to associate with node
 * @param <K> enumeration type used as key
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public interface EnumTreeNode<I, K extends Enum<K>, D> {
    /**
     * Get identifier of this node. ID is guaranteed to be unique in context of
     * parent. That means that there can be multiple nodes with same ID within
     * tree, but with different parent.
     *
     * @return identifier of this node
     */
    I id();

    /**
     * Children of this node.
     *
     * @return read-only view of children of this node.
     */
    Collection<EnumTreeNode<I, K, D>> children();

    /**
     * Attempts to find direct child of this node based on its ID.
     *
     * @param childId ID of child node
     * @return {@link Optional} of child
     */
    Optional<EnumTreeNode<I, K, D>> lookupChild(I childId);

    /**
     * Set value associated with this node.
     *
     * @param key enumeration key used to specify type of data
     * @param data data to associate with this node
     */
    void setValue(K key, D data);

    /**
     * Return data associated with this node, or null if no such data exists.
     *
     * @param key enumeration key used to specify type of data
     * @return associated data of given type or null.
     */
    D value(K key);

    /**
     * Get read-only view of all values as map, where key is enumeration type
     * and value is actual value. Value can be null, which means that there are
     * no data associated with given enumeration key.
     *
     * @return associated data as map
     */
    Map<K, D> allValues();

    /**
     * Appends child node to this node with given node identifier.
     *
     * @param id child node identifier
     * @return appended child {@link EnumTreeNode}
     */
    EnumTreeNode<I, K, D> appendChild(I id);
}
