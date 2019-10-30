/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import com.google.gson.JsonElement;

import java.util.List;

/**
 * Governance operations.
 */
public interface RemoteGovernance extends AutoCloseable {
    /**
     * Find endpoint uri of the service which governs the data store subtree specified by path for a named entity.
     *
     * @param store data store that is the subject of this procedure
     * @param entity name of the managed entity that is the subject of this procedure
     * @param path path specifying the model subtree that is the subject of this procedure
     * @return endpoint uri of the service which governs the data store subtree or null if there is no such mapping
     */
    default String governance(int store, String entity, JsonElement path) {
        return governance(String.valueOf(store), entity, path);
    }

    /**
     * Find endpoint uri of the service which governs the data store subtree specified by path for a named entity.
     *
     * @param store data store that is the subject of this procedure
     * @param entity name of the managed entity that is the subject of this procedure
     * @param path path specifying the model subtree that is the subject of this procedure
     * @return endpoint uri of the service which governs the data store subtree or null if there is no such mapping
     */
    default String governance(String store, String entity, JsonElement path) {
        return governance(new StoreOperationArgument(store, entity, path));
    }

    /**
     * Find endpoint uri of the service which governs the data store subtree specified by path for a named entity.
     *
     * @param arg governance operation argument
     * @return endpoint uri of the service which governs the data store subtree or null if there is no such mapping
     * @see #governance(String, String, JsonElement)
     * @see #governance(int, String, JsonElement)
     */
    String governance(StoreOperationArgument arg);

    /**
     * Return the YANG source text of the specified YANG module.
     *
     * @param arg source operation argument
     * @return YANG source text or null if no such module is found
     * @see #source(String, String)
     */
    String source(ModuleInfo arg);

    /**
     * Return the YANG source text of the specified YANG module.
     *
     * @param module name of module
     * @param revision revision of module
     * @return YANG source text or null if no such module is found
     */
    default String source(String module, String revision) {
        return source(new ModuleInfo(module, revision));
    }

    /**
     * Return the YANG source text of the specified YANG module.
     *
     * @param module name of module
     * @return YANG source text or null if no such module is found
     */
    default String source(String module) {
        return source(module, null);
    }

    /**
     * Get list of all dependencies for given module.
     *
     * @param arg depends operation argument
     * @return list of all module dependencies resolved recursively
     */
    List<ModuleInfo> depends(ModuleInfo arg);

    /**
     * Get list of all dependencies for given module.
     *
     * @param module name of module
     * @param revision revision of module
     * @return list of all module dependencies resolved recursively
     */
    default List<ModuleInfo> depends(String module, String revision) {
        return depends(new ModuleInfo(module, revision));
    }
}
