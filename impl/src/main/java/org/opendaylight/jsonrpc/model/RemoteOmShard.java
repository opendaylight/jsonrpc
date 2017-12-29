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

/* DOM Broker operations */
public interface RemoteOmShard extends AutoCloseable {

    JsonElement read(int store, String entity, JsonElement path) throws Exception;

    JsonElement read(String store, String entity, JsonElement path) throws Exception;

    void put(String txId, int store, String entity, JsonElement path, JsonElement data);

    void put(String txId, String store, String entity, JsonElement path, JsonElement data);

    boolean exists(int store, String entity, JsonElement path) throws Exception;

    boolean exists(String store, String entity, JsonElement path) throws Exception;

    void merge(String txId, int store, String entity, JsonElement path, JsonElement data);

    void merge(String txId, String store, String entity, JsonElement path, JsonElement data);

    void delete(String txId, int store, String entity, JsonElement path);

    void delete(String txId, String store, String entity, JsonElement path);

    boolean commit(String txId);

    boolean cancel(String txId);

    String txid();

    List<String> error(String txId);
}
