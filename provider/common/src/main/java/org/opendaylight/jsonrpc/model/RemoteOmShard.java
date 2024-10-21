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
 * DOM data broker operations.
 *
 * <p>Documented at <a href= "https://wiki.opendaylight.org/view/JSON-RPC2.0::ExtendingTheDataStore">wiki</a>
 */
public interface RemoteOmShard extends AutoCloseable {
    /**
     * Read all data at path in the given data store for an entity.
     *
     * @param arg read operation argument
     * @return all data at path in the given data store for an entity
     * @see #read(String, String, JsonElement)
     */
    JsonElement read(StoreOperationArgument arg);

    /**
     * Read all data at path in the given data store for an entity.
     *
     * @param store data store that is the subject of this procedure ("operational" or "config")
     * @param entity name of the managed entity that is the subject of this procedure
     * @param path path specifying the model subtree that is the subject of this procedure
     * @return all data at path in the given data store for an entity
     */
    default JsonElement read(String store, String entity, JsonElement path) {
        return read(new StoreOperationArgument(store, entity, path));
    }

    /**
     * Read all data at path in the given data store for an entity.
     *
     * @param store data store that is the subject of this procedure (0 for config, 1 for operational)
     * @param entity name of the managed entity that is the subject of this procedure
     * @param path path specifying the model subtree that is the subject of this procedure
     * @return all data at path in the given data store for an entity
     */
    default JsonElement read(int store, String entity, JsonElement path) {
        return read(String.valueOf(store), entity, path);
    }

    /**
     * Store data at path in the given data store for an entity. This procedure will overwrite any and all existing
     * data at path, when the transaction to which it belongs is committed.
     *
     * @param arg put operation argument
     */
    void put(DataOperationArgument arg);

    /**
     * Store data at path in the given data store for an entity. This procedure will overwrite any and all existing
     * data at path, when the transaction to which it belongs is committed.
     *
     * @param txId handle for a transaction
     * @param store data store that is the subject of this procedure ("operational" or "config")
     * @param entity name of the managed entity that is the subject of this procedure
     * @param path path specifying the model subtree that is the subject of this procedure
     * @param data data to write
     */
    default void put(String txId, String store, String entity, JsonElement path, JsonElement data) {
        put(new DataOperationArgument(txId, store, entity, path, data));
    }

    /**
     * Store data at path in the given data store for an entity. This procedure will overwrite any and all existing
     * data at path, when the transaction to which it belongs is committed.
     *
     * @param txId handle for a transaction
     * @param store data store that is the subject of this procedure (0 for config, 1 for operational)
     * @param entity name of the managed entity that is the subject of this procedure
     * @param path path specifying the model subtree that is the subject of this procedure
     * @param data data to write
     */
    default void put(String txId, int store, String entity, JsonElement path, JsonElement data) {
        put(txId, String.valueOf(store), entity, path, data);
    }

    /**
     * Check whether any data is available at path in the given data store for an entity.
     *
     * @param arg 'exists' operation argument
     * @return true if data exists, false otherwise
     * @see #exists(String, String, JsonElement)
     */
    boolean exists(StoreOperationArgument arg);

    /**
     * Check whether any data is available at path in the given data store for an entity.
     *
     * @param store data store that is the subject of this procedure ("operational" or "config")
     * @param entity name of the managed entity that is the subject of this procedure
     * @param path path specifying the model subtree that is the subject of this procedure
     * @return true if data exists, false otherwise
     */
    default boolean exists(String store, String entity, JsonElement path) {
        return exists(new StoreOperationArgument(store, entity, path));
    }

    /**
     * Check whether any data is available at path in the given data store for an entity.
     *
     * @param store data store that is the subject of this procedure (0 for config, 1 for operational)
     * @param entity name of the managed entity that is the subject of this procedure
     * @param path path specifying the model subtree that is the subject of this procedure
     * @return true if data exists, false otherwise
     */
    default boolean exists(int store, String entity, JsonElement path) {
        return exists(String.valueOf(store), entity, path);
    }

    /**
     * Store data at path in the given data store for an entity. This procedure merges this new data with any existing
     * data at path, with the new data overriding, when the transaction to which it belongs is committed.
     *
     * @param arg merge operation argument
     */
    void merge(DataOperationArgument arg);

    /**
     * Store data at path in the given data store for an entity. This procedure merges this new data with any existing
     * data at path, with the new data overriding, when the transaction to which it belongs is committed.
     *
     * @param txId handle for a transaction
     * @param store data store that is the subject of this procedure ("operational" or "config")
     * @param entity name of the managed entity that is the subject of this procedure
     * @param path path specifying the model subtree that is the subject of this procedure
     * @param data data to merge
     */
    default void merge(String txId, String store, String entity, JsonElement path, JsonElement data) {
        merge(new DataOperationArgument(txId, store, entity, path, data));
    }

    /**
     * Store data at path in the given data store for an entity. This procedure merges this new data with any existing
     * data at path, with the new data overriding, when the transaction to which it belongs is committed.
     *
     * @param txId handle for a transaction
     * @param store data store that is the subject of this procedure (0 for config, 1 for operational)
     * @param entity name of the managed entity that is the subject of this procedure
     * @param path path specifying the model subtree that is the subject of this procedure
     * @param data data to merge
     */
    default void merge(String txId, int store, String entity, JsonElement path, JsonElement data) {
        merge(txId, String.valueOf(store), entity, path, data);
    }

    /**
     * Delete all data at path in the given data store for an entity. This procedure deletes all data at path when the
     * transaction to which it belongs is committed.
     *
     * @param arg delete operation argument
     */
    void delete(TxOperationArgument arg);

    /**
     * Delete all data at path in the given data store for an entity. This procedure deletes all data at path when the
     * transaction to which it belongs is committed.
     *
     * @param txId handle for a transaction
     * @param store data store that is the subject of this procedure ("config" or "operational")
     * @param entity name of the managed entity that is the subject of this procedure
     * @param path path specifying the model subtree that is the subject of this procedure
     */
    default void delete(String txId, String store, String entity, JsonElement path) {
        delete(new TxOperationArgument(txId, store, entity, path));
    }

    /**
     * Delete all data at path in the given data store for an entity. This procedure deletes all data at path when the
     * transaction to which it belongs is committed.
     *
     * @param txId handle for a transaction
     * @param store data store that is the subject of this procedure (0 for config, 1 for operational)
     * @param entity name of the managed entity that is the subject of this procedure
     * @param path path specifying the model subtree that is the subject of this procedure
     */
    default void delete(String txId, int store, String entity, JsonElement path) {
        delete(txId, String.valueOf(store), entity, path);
    }

    /**
     * Make permanent all data changes made in the identified transaction, then end that transaction.
     *
     * @param arg commit operation argument
     * @return true if all changes were committed and that the transaction has ended, false otherwise
     */
    boolean commit(TxArgument arg);

    /**
     * Make permanent all data changes made in the identified transaction, then end that transaction.
     *
     * @param txId handle for a transaction
     * @return true if all changes were committed and that the transaction has ended, false otherwise
     */
    default boolean commit(String txId) {
        return commit(new TxArgument(txId));
    }

    /**
     * Discard all data changes made in the identified transaction, then end that transaction.
     *
     * @param arg cancel operation argument
     * @return true if transaction has ended, false otherwise.
     */
    boolean cancel(TxArgument arg);

    /**
     * Discard all data changes made in the identified transaction, then end that transaction.
     *
     * @param txId handle for a transaction
     * @return true if transaction has ended, false otherwise.
     */
    default boolean cancel(String txId) {
        return cancel(new TxArgument(txId));
    }

    /**
     * Allocate locally unique transaction handle.
     *
     * @return new transaction handle
     */
    String txid();

    /**
     * List of failures that might happened during commit operation.
     *
     * @param arg error operation argument
     * @return list of error messages, never NULL.
     */
    List<String> error(TxArgument arg);

    /**
     * List of failures that might happened during commit operation.
     *
     * @param txId handle for transaction
     * @return list of error messages, never NULL.
     */
    default List<String> error(String txId) {
        return error(new TxArgument(txId));
    }

    /**
     * Add a data change listener for a path.
     *
     * @param arg add-listener operation argument
     * @return instance of {@link ListenerKey}.
     * @throws IOException when publisher socket can't be created
     */
    ListenerKey addListener(AddListenerArgument arg) throws IOException;

    /**
     * Add a data change listener for a path.
     *
     * @param store data store that is the subject of this procedure ("operational" or "config")
     * @param entity name of the managed entity that is the subject of this procedure
     * @param path data tree path at which to add listener for data changes
     * @param transport (protocol) to use to communicate changes
     * @return instance of {@link ListenerKey}.
     * @throws IOException when publisher socket can't be created
     */
    default ListenerKey addListener(String store, String entity, JsonElement path, String transport)
            throws IOException {
        return addListener(new AddListenerArgument(store, entity, path, transport));
    }

    /**
     * Add a data change listener for a path.
     *
     * @param store data store that is the subject of this procedure ("operational" or "config")
     * @param entity name of the managed entity that is the subject of this procedure
     * @param path data tree path at which to add listener for data changes
     * @return instance of {@link ListenerKey}.
     * @throws IOException when publisher socket can't be created
     */
    default ListenerKey addListener(String store, String entity, JsonElement path) throws IOException {
        return addListener(new AddListenerArgument(store, entity, path, null));
    }

    /**
     * Add a data change listener for a path.
     *
     * @param store data store that is the subject of this procedure (0 for config, 1 for operational)
     * @param entity name of the managed entity that is the subject of this procedure
     * @param path data tree path at which to add listener for data changes
     * @param transport (protocol) to use to communicate changes
     * @return instance of {@link ListenerKey}.
     * @throws IOException when publisher socket can't be created
     */
    default ListenerKey addListener(int store, String entity, JsonElement path, String transport) throws IOException {
        return addListener(String.valueOf(store), entity, path, transport);
    }

    /**
     * Add a data change listener for a path.
     *
     * @param store data store that is the subject of this procedure (0 for config, 1 for operational)
     * @param entity name of the managed entity that is the subject of this procedure
     * @param path data tree path at which to add listener for data changes
     * @return instance of {@link ListenerKey}.
     * @throws IOException when publisher socket can't be created
     */
    default ListenerKey addListener(int store, String entity, JsonElement path) throws IOException {
        return addListener(String.valueOf(store), entity, path, null);
    }

    /**
     * Delete data change listener.
     *
     * @param arg delete-listener operation argument
     * @return true if removal was successful, false otherwise
     */
    boolean deleteListener(DeleteListenerArgument arg);

    /**
     * Delete data change listener.
     *
     * @param uri URI obtained from {@link #addListener(AddListenerArgument)} call
     * @param dcName name obtained from {@link #addListener(AddListenerArgument)} call
     * @return true if removal was successful, false otherwise
     */
    default boolean deleteListener(String uri, String dcName) {
        return deleteListener(new DeleteListenerArgument(uri, dcName));
    }

    /**
     * Overridden to deal with throw declaration which is unfriendly with java 8 streams. This method is called when
     * remote shard is no longer needed.
     */
    @Override
    void close();
}
