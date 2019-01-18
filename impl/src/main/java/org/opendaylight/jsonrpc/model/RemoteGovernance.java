/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import java.util.List;

/**
 * Governance operations.
 */
public interface RemoteGovernance extends AutoCloseable {
    /**
     * Find endpoint uri of the service which governs the data store subtree
     * specified by path for a named entity.
     *
     * @param store data store that is the subject of this procedure
     * @param entity name of the managed entity that is the subject of this
     *            procedure
     * @param path path specifying the model subtree that is the subject of this
     *            procedure
     * @return endpoint uri of the service which governs the data store subtree
     *         or null if there is no such mapping
     */
    String governance(int store, String entity, Object path);

    /**
     * Find endpoint uri of the service which governs the data store subtree
     * specified by path for a named entity.
     *
     * @param store data store that is the subject of this procedure
     * @param entity name of the managed entity that is the subject of this
     *            procedure
     * @param path path specifying the model subtree that is the subject of this
     *            procedure
     * @return endpoint uri of the service which governs the data store subtree
     *         or null if there is no such mapping
     */
    String governance(String store, String entity, Object path);

    /**
     * Return the YANG source text of the specified YANG module.
     *
     * @param name name of YANG module
     * @return YANG source text or null if no such module is found
     */
    String source(String name);

    /**
     * Return the YANG source text of the specified YANG module.
     *
     * @param name YANG module name
     * @param revision YANG module revision
     * @return YANG source text or null if no such module is found
     */
    String source(String name, String revision);

    /**
     * Get list of all dependencies for given module.
     *
     * @param moduleName name of module
     * @param revision revision of module
     * @return list of all module dependencies resolved recursively
     */
    List<ModuleInfo> depends(String moduleName, String revision);
}
