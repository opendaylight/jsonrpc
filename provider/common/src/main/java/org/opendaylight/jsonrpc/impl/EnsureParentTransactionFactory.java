/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.model.TransactionFactory;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.spi.ForwardingDOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Implementation of {@link TransactionFactory} which follows semantics of {@link AbstractWriteTransaction}.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
class EnsureParentTransactionFactory implements TransactionFactory {
    private final SchemaContext schemaContext;
    protected final DOMDataBroker domDataBroker;

    EnsureParentTransactionFactory(final DOMDataBroker domDataBroker, final SchemaContext schemaContext) {
        this.domDataBroker = domDataBroker;
        this.schemaContext = schemaContext;
    }

    @Override
    public DOMDataTreeWriteTransaction get() {
        final DOMDataTreeWriteTransaction delegateTx = domDataBroker.newWriteOnlyTransaction();
        return new ForwardingDOMDataWriteTransaction() {
            @Override
            public void merge(LogicalDatastoreType store, YangInstanceIdentifier path, NormalizedNode data) {
                final List<PathArgument> normalizedPathWithoutChildArgs = new ArrayList<>();
                YangInstanceIdentifier rootNormalizedPath = null;
                final Iterator<PathArgument> it = path.getPathArguments().iterator();
                while (it.hasNext()) {
                    final PathArgument pathArgument = it.next();
                    if (rootNormalizedPath == null) {
                        rootNormalizedPath = YangInstanceIdentifier.create(pathArgument);
                    }
                    if (it.hasNext()) {
                        normalizedPathWithoutChildArgs.add(pathArgument);
                    }
                }
                if (normalizedPathWithoutChildArgs.isEmpty()) {
                    return;
                }
                Preconditions.checkArgument(rootNormalizedPath != null, "Empty path received");
                final NormalizedNode parentStructure = ImmutableNodes.fromInstanceId(schemaContext,
                        YangInstanceIdentifier.create(normalizedPathWithoutChildArgs));
                delegate().merge(store, rootNormalizedPath, parentStructure);
            }

            @Override
            protected @NonNull DOMDataTreeWriteTransaction delegate() {
                return delegateTx;
            }
        };
    }
}