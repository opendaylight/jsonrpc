/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import com.google.gson.JsonElement;

import java.io.IOException;
import java.util.List;

/**
 * DOM Broker operations.
 *
 * <p>
 * Documented at <a href=
 * "https://wiki.opendaylight.org/view/JSON-RPC2.0::ExtendingTheDataStore">wiki</a>
 */
public interface RemoteOmShard extends AutoCloseable {
    /**
     * Read all data at path in the given data store for an entity.
     *
     * @param store data store that is the subject of this procedure (0 for
     *            config, 1 for operational)
     * @param entity name of the managed entity that is the subject of this
     *            procedure
     * @param path path specifying the model subtree that is the subject of this
     *            procedure
     * @return all data at path in the given data store for an entity
     */
    JsonElement read(int store, String entity, JsonElement path);

    /**
     * Read all data at path in the given data store for an entity.
     *
     * @param store data store that is the subject of this procedure
     *            ("operational" or "config")
     * @param entity name of the managed entity that is the subject of this
     *            procedure
     * @param path path specifying the model subtree that is the subject of this
     *            procedure
     * @return all data at path in the given data store for an entity
     */
    JsonElement read(String store, String entity, JsonElement path);

    /**
     * Store data at path in the given data store for an entity. This procedure
     * will overwrite any and all existing data at path, when the transaction to
     * which it belongs is committed.
     *
     * @param txId handle for a transaction
     * @param store data store that is the subject of this procedure (0 for
     *            config, 1 for operational)
     * @param entity name of the managed entity that is the subject of this
     *            procedure
     * @param path path specifying the model subtree that is the subject of this
     *            procedure
     * @param data data to write
     */
    void put(String txId, int store, String entity, JsonElement path, JsonElement data);

    /**
     * Store data at path in the given data store for an entity. This procedure
     * will overwrite any and all existing data at path, when the transaction to
     * which it belongs is committed.
     *
     * @param txId handle for a transaction
     * @param store data store that is the subject of this procedure
     *            ("operational" or "config")
     * @param entity name of the managed entity that is the subject of this
     *            procedure
     * @param path path specifying the model subtree that is the subject of this
     *            procedure
     * @param data data to write
     */
    void put(String txId, String store, String entity, JsonElement path, JsonElement data);

    /**
     * Check whether any data is available at path in the given data store for
     * an entity.
     *
     * @param store data store that is the subject of this procedure (0 for
     *            config, 1 for operational)
     * @param entity name of the managed entity that is the subject of this
     *            procedure
     * @param path path specifying the model subtree that is the subject of this
     *            procedure
     * @return true if data exists, false otherwise
     */
    boolean exists(int store, String entity, JsonElement path);

    /**
     * Check whether any data is available at path in the given data store for
     * an entity.
     *
     * @param store data store that is the subject of this procedure
     *            ("operational" or "config")
     * @param entity name of the managed entity that is the subject of this
     *            procedure
     * @param path path specifying the model subtree that is the subject of this
     *            procedure
     * @return true if data exists, false otherwise
     */
    boolean exists(String store, String entity, JsonElement path);

    /**
     * Store data at path in the given data store for an entity. This procedure
     * merges this new data with any existing data at path, with the new data
     * overriding, when the transaction to which it belongs is committed.
     *
     * @param txId handle for a transaction
     * @param store data store that is the subject of this procedure (0 for
     *            config, 1 for operational)
     * @param entity name of the managed entity that is the subject of this
     *            procedure
     * @param path path specifying the model subtree that is the subject of this
     *            procedure
     * @param data data to merge
     */
    void merge(String txId, int store, String entity, JsonElement path, JsonElement data);

    /**
     * Store data at path in the given data store for an entity. This procedure
     * merges this new data with any existing data at path, with the new data
     * overriding, when the transaction to which it belongs is committed.
     *
     * @param txId handle for a transaction
     * @param store data store that is the subject of this procedure
     *            ("operational" or "config")
     * @param entity name of the managed entity that is the subject of this
     *            procedure
     * @param path path specifying the model subtree that is the subject of this
     *            procedure
     * @param data data to merge
     */
    void merge(String txId, String store, String entity, JsonElement path, JsonElement data);

    /**
     * Delete all data at path in the given data store for an entity. This
     * procedure deletes all data at path when the transaction to which it
     * belongs is committed.
     *
     * @param txId handle for a transaction
     * @param store data store that is the subject of this procedure (0 for
     *            config, 1 for operational)
     * @param entity name of the managed entity that is the subject of this
     *            procedure
     * @param path path specifying the model subtree that is the subject of this
     *            procedure
     */
    void delete(String txId, int store, String entity, JsonElement path);

    /**
     * Delete all data at path in the given data store for an entity. This
     * procedure deletes all data at path when the transaction to which it
     * belongs is committed.
     *
     * @param txId handle for a transaction
     * @param store data store that is the subject of this procedure
     *            ("operational" or "config")
     * @param entity name of the managed entity that is the subject of this
     *            procedure
     * @param path path specifying the model subtree that is the subject of this
     *            procedure
     */
    void delete(String txId, String store, String entity, JsonElement path);

    /**
     * Make permanent all data changes made in the identified transaction, then
     * end that transaction.
     *
     * @param txId handle for a transaction
     * @return true if all changes were committed and that the transaction has
     *         ended, false otherwise
     */
    boolean commit(String txId);

    /**
     * Discard all data changes made in the identified transaction, then end
     * that transaction.
     *
     * @param txId handle for a transaction
     * @return true if transaction has ended, false otherwise.
     */
    boolean cancel(String txId);

    /**
     * Allocate locally unique transaction handle.
     *
     * @return new transaction handle
     */
    String txid();

    /**
     * List of failures that might happened during commit operation.
     *
     * @param txId handle for transaction
     * @return list of error messages, never NULL.
     */
    List<String> error(String txId);

    /**
     * Add a data change listener for a path.
     *
     * @param store data store that is the subject of this procedure (0 for
     *            config, 1 for operational)
     * @param entity name of the managed entity that is the subject of this
     *            procedure
     * @param path data tree path at which to add listener for data changes
     * @return instance of {@link ListenerKey}.
     * @throws IOException when publisher socket can't be created
     */
    ListenerKey addListener(int store, String entity, JsonElement path) throws IOException;

    /**
     * Add a data change listener for a path.
     *
     * @param store data store that is the subject of this procedure (0 for
     *            config, 1 for operational)
     * @param entity name of the managed entity that is the subject of this
     *            procedure
     * @param path data tree path at which to add listener for data changes
     * @param transport (protocol) to use to communicate changes
     * @return instance of {@link ListenerKey}.
     * @throws IOException when publisher socket can't be created
     */
    ListenerKey addListener(int store, String entity, JsonElement path, String transport) throws IOException;

    /**
     * Add a data change listener for a path.
     *
     * @param store data store that is the subject of this procedure
     *            ("operational" or "config")
     * @param entity name of the managed entity that is the subject of this
     *            procedure
     * @param path data tree path at which to add listener for data changes
     * @return instance of {@link ListenerKey}.
     * @throws IOException when publisher socket can't be created
     */
    ListenerKey addListener(String store, String entity, JsonElement path) throws IOException;

    /**
     * Add a data change listener for a path.
     *
     * @param store data store that is the subject of this procedure
     *            ("operational" or "config")
     * @param entity name of the managed entity that is the subject of this
     *            procedure
     * @param path data tree path at which to add listener for data changes
     * @param transport (protocol) to use to communicate changes
     * @return instance of {@link ListenerKey}.
     * @throws IOException when publisher socket can't be created
     */
    ListenerKey addListener(String store, String entity, JsonElement path, String transport) throws IOException;

    /**
     * Delete data change listener.
     *
     * @param uri URI obtained from
     *            {@link #addListener(int, String, JsonElement)} call
     * @param dcName name obtained from
     *            {@link #addListener(int, String, JsonElement)} call
     * @return true if removal was successful, false otherwise
     */
    boolean deleteListener(String uri, String dcName);

    /**
     * Overridden to deal with throw declaration which is unfriendly with java 8
     * streams. This method is called when remote shard is no longer needed.
     */
    @Override
    void close();
}
