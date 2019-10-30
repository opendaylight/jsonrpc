/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.opendaylight.jsonrpc.model.ModuleInfo;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.model.StoreOperationArgument;

/**
 * Mock implementation of {@link RemoteGovernance}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 22, 2018
 */
public class MockGovernance implements RemoteGovernance {

    @Override
    public void close() throws Exception {
        // NOOP
    }

    @Override
    public String governance(StoreOperationArgument arg) {
        // NOOP
        return null;
    }

    @Override
    public String source(ModuleInfo arg) {
        try (InputStream is = Resources.getResource(getClass(), "/" + arg.getModule() + ".yang").openStream()) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ByteStreams.copy(is, os);
            return new String(os.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<ModuleInfo> depends(ModuleInfo arg) {
        return Lists.newArrayList(new ModuleInfo(arg.getModule(), arg.getRevision()));
    }
}
