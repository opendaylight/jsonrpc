/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.hmap;

import java.util.Map;
import java.util.Optional;

/**
 * Hierarchical tree structure which can associate data to node at any given
 * path within tree.
 *
 * @param <P> type of path used to denote location within logical tree (aka
 *            external path specification)
 * @param <K> enum type used as key
 * @param <D> type of data to associate with path in data tree
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public interface HierarchicalEnumMap<P, K extends Enum<K>, D> {
    /**
     * Performs lookup of data at given path. Tree nodes are allocated from root
     * down the path until leaf, if they not exists. When no data are associated
     * with leaf(that is target node has null value), tree is traversed up to
     * root for any non-null value.If no data are found, then result is
     * {@link Optional#empty()}
     *
     * @param path path to data within logical tree
     * @param key enum key used to specify type of data
     * @return {@link Optional} of D
     */
    Optional<D> lookup(P path, K key);

    /**
     * Put data into node at given path. Tree nodes are allocated from root
     * until leaf is encountered, if they not exists. If there was data already
     * associated with node, they are overwritten.
     *
     * @param path path to data within logical tree
     * @param key enum key used to specify type of data
     * @param data to associate with node at given path, can be null
     * @return D if there was data associated with node at given path.
     */
    D put(P path, K key, D data);

    /**
     * Create read-only view of effective data tree. Paths which not resolves to
     * non-null data are skipped.
     *
     * @param key enum key used to specify type of data
     * @return effective data tree in for of {@link Map} where key is of type
     *         {@link P} and value is {@link D}
     */
    Map<P, D> toMap(K key);

    /**
     * Diagnostic/helper method to provide textual representation of tree state.
     *
     * @return textual representation of data tree.
     */
    String dump();
}
