/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.util.concurrent.EventExecutorGroup;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is providing customized {@link ThreadFactory} for different {@link EventExecutorGroup} used by bus.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Oct 5, 2018
 */
public final class ThreadFactoryProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ThreadFactoryProvider.class);
    private static final UncaughtExceptionHandler ERROR_HANDLER = (thread, cause) -> LOG
            .error("Uncaught error in thread {}", thread, cause);

    private ThreadFactoryProvider() {
        // no instantiation
    }

    /**
     * Create {@link ThreadFactory} that names thread with given prefix.
     *
     * @param prefix thread name prefix
     * @return customized {@link ThreadFactory}
     */
    public static ThreadFactory create(String prefix) {
        return new ThreadFactoryBuilder().setDaemon(true)
                .setNameFormat("jsonrpc-" + prefix + "-eventloop-%d")
                .setUncaughtExceptionHandler(ERROR_HANDLER)
                .build();
    }
}
