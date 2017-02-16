/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

/* Governance operations */
public interface RemoteGovernance extends AutoCloseable{

    String governance(int store, String entity, Object path);

    String source(String name);
    String source(String name, String revision);
}
