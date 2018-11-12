/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

/**
 * Common constants shared between message library classes.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Nov 16, 2018
 */
public final class MessageLibraryConstants {
    /**
     * Default value of retry attempts when invoking request via proxy service.
     */
    public static final int DEFAULT_PROXY_RETRY_COUNT = 5;

    /**
     * Default value of retry delay between proxy method invocations.
     */
    public static final long DEFAULT_PROXY_RETRY_DELAY = 100;

    /**
     * Default value of skipping endpoint caching.
     */
    public static final boolean DEFAULT_SKIP_ENDPOINT_CACHE = true;

    /**
     * Default value of request timeout.
     */
    public static final int DEFAULT_TIMEOUT = 30_000;

    /**
     * Name of query parameter used to propagate timeout value.
     */
    public static final String PARAM_TIMEOUT = "timeout";

    /**
     * Name of query parameter used to propagate proxy retry count.
     */
    public static final String PARAM_PROXY_RETRY_COUNT = "proxyRetryCount";

    /**
     * Name of query parameter used to propagate proxy retry count.
     */
    public static final String PARAM_PROXY_RETRY_DELAY = "proxyRetryDelay";

    private MessageLibraryConstants() {
        // prevent instantiation of this class
    }

}
