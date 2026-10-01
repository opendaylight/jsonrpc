/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.ServiceLoader;
import org.opendaylight.jsonrpc.model.InbandModelsService;
import org.opendaylight.jsonrpc.model.Module;
import org.opendaylight.yangtools.binding.meta.YangModelBindingProvider;

/**
 * Convenient class meant to be extended by standalone applications running outside of controller.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jan 12, 2019
 */
public abstract class AbstractInbandModelsService implements InbandModelsService {
    private static final List<Module> MODULES = ServiceLoader.load(YangModelBindingProvider.class).stream()
        .map(provider -> provider.get().getModuleInfo())
        .filter(ymi -> !ymi.name().getLocalName().startsWith("jsonrpc"))
        .map(ymi -> {
            try {
                return new Module(ymi.name().getLocalName(), ymi.getYangTextCharSource().read());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        })
        .collect(ImmutableList.toImmutableList());

    @Override
    public List<Module> getModules() {
        return MODULES;
    }

    @Override
    public void close() {
        // NOOP
    }
}
