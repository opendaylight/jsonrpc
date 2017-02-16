/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * {@link org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap} is data structure
 * which can associate vector values to any given data node specified by path
 * within tree.
 *
 * Data are associated into {@link org.opendaylight.jsonrpc.hmap.EnumTreeNode},
 * which represents point within data tree.
 *
 * {@link org.opendaylight.jsonrpc.hmap.PathCodec} is bi-directional API to
 * convert between internal node identifier and external path specification.
 * Path is sequence of node identifier, with first identifier equals to null.
 *
 * <p>
 * Example code
 * </p>
 * <blockquote>
 *
 * <pre>
 *
 * JsonPathCodec codec = JsonPathCodec.create();
 * HierarchicalEnumMap<JsonElement, Types, String, String> map = HierarchicalEnumHashMap.create(Types.class, codec);
 *
 * map.lookup(path, Types.TYPE_A); // perform lookup
 *
 * map.put(path, Types.TYPE_B, "Some data to store"); // store data
 * </pre>
 *
 * </blockquote>
 * 
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
package org.opendaylight.jsonrpc.hmap;
