/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.tool.test;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.opendaylight.jsonrpc.bus.messagelib.DefaultTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.provider.common.GovernanceSchemaContextProvider;
import org.opendaylight.jsonrpc.tool.test.Parameters.Options;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
import org.opendaylight.yangtools.yang.xpath.impl.AntlrXPathParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private final String[] args;

    private Main(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) throws URISyntaxException {
        new Main(args).run();
    }

    @SuppressFBWarnings("DM_EXIT")
    private void run() throws URISyntaxException {
        try {
            final Options opts = Parameters.createArgParser(args);
            final TransportFactory tf = new DefaultTransportFactory();
            final Path yangDir = Paths.get(opts.yangDirectory);
            final GovernanceImpl governance = new GovernanceImpl(tf, opts.governance, yangDir);
            LOG.info("Started : {}", governance);
            if (opts.datastore != null) {
                Preconditions.checkArgument(opts.datastoreModules != null,
                        "Argument 'datastore-modules' is required if --datastore option is provided");
                final List<String> modules = Lists.newArrayList(opts.datastoreModules.split(","));
                if (opts.rpc != null) {
                    modules.add("test-model-rpc");
                }
                LOG.info("Datastore modules : {}", modules);
                final DatastoreImpl datastore = DatastoreImpl.create(tf, opts.datastore,
                        modules.stream().map(YangIdentifier::new).collect(Collectors.toSet()),
                        new GovernanceSchemaContextProvider(governance, new AntlrXPathParserFactory()));
                LOG.info("Started : {}", datastore);
            }
            if (opts.rpc != null) {
                final TestModelImpl testService = new TestModelImpl(tf, opts.rpc);
                LOG.info("Started : {}", testService);
            }
            waitForever();
        } catch (ArgumentParserException e) {
            LOG.error("Invalid arguments", e);
            System.exit(1);
        }
    }

    private static void waitForever() {
        Uninterruptibles.awaitUninterruptibly(new CountDownLatch(1));
    }
}
