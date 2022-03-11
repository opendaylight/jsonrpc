/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opendaylight.jsonrpc.model.TransactionFactory;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

/**
 * Helper to deal with transaction lifecycle.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 2, 2020
 */
public class TransactionManager implements AutoCloseable {
    private final TransactionFactory txFactory;
    private static final long FAIL_TRX_TTL = 900000; // 15 minutes

    private LoadingCache<String, DataModificationContext> txLoader = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, DataModificationContext>() {
                @Override
                public DataModificationContext load(String key) throws Exception {
                    return new DataModificationContext(txFactory);
                }
            });

    public TransactionManager(final DOMDataBroker dataBroker, final EffectiveModelContext schemaContext) {
        txFactory = new EnsureParentTransactionFactory(dataBroker, schemaContext);
    }

    public Entry<String, DataModificationContext> allocate(String id) {
        evictFailedTrx();
        final String txid = Optional.ofNullable(id).orElse(UUID.randomUUID().toString());
        return new AbstractMap.SimpleEntry<>(txid, txLoader.getUnchecked(txid));
    }

    public boolean commit(String id) {
        return removeUponSuccess(id, DataModificationContext::submit);
    }

    public boolean cancel(String id) {
        return removeUponSuccess(id, DataModificationContext::cancel);
    }

    public List<String> error(String id) {
        final DataModificationContext ctx = txLoader.asMap().get(id);
        if (ctx == null) {
            return Collections.emptyList();
        }
        return ctx.getErrors().stream().map(TransactionManager::serializeError).collect(Collectors.toList());
    }

    private void evictFailedTrx() {
        txLoader.asMap().entrySet().removeIf(TransactionManager::removeFailedTrxPredicate);
    }

    private static boolean removeFailedTrxPredicate(Entry<String, DataModificationContext> entry) {
        return !entry.getValue().isSuccess()
                && entry.getValue().getCompletionTimestamp() + FAIL_TRX_TTL > System.currentTimeMillis();
    }

    private boolean removeUponSuccess(String id, Predicate<DataModificationContext> mapper) {
        if (Optional.ofNullable(txLoader.asMap().get(id)).map(mapper::test).orElse(false)) {
            txLoader.asMap().remove(id);
            return true;
        }
        return false;
    }

    private static String serializeError(final Throwable error) {
        return Throwables.getRootCause(error).getMessage();
    }

    @Override
    public void close() {
        txLoader.asMap().values().forEach(DataModificationContext::cancel);
        txLoader.asMap().clear();
    }
}
