/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import java.util.List;
import org.opendaylight.jsonrpc.bus.api.RpcMethod;

/**
 * API for inband models which allows to setup remote endpoint without governance.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jan 12, 2019
 */
public interface InbandModelsService extends AutoCloseable {
    /**
     * Get list of all YANG modules for this service.
     *
     * @return list of modules
     */
    @RpcMethod("get-modules")
    List<Module> getModules();
}
