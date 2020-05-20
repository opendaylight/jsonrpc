/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.tool.test;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

final class Parameters {
    private Parameters() {
        // noop
    }

    static final class Options {
        @Arg(dest = "governance")
        public String governance;

        @Arg(dest = "yang-directory")
        public String yangDirectory;

        @Arg(dest = "datastore")
        public String datastore;

        @Arg(dest = "datastore-modules")
        public String datastoreModules;

        @Arg(dest = "rpc")
        public String rpc;
    }

    static Options createArgParser(String[] args) throws ArgumentParserException {
        final ArgumentParser parser = ArgumentParsers.newFor("jsonrpc-testtool").addHelp(true).build();

        parser.addArgument("--governance")
                .type(String.class)
                .help("Local endpoint to bind governance service responder to")
                .dest("governance");

        parser.addArgument("--datastore")
                .type(String.class)
                .help("Local endpoint to bind datastore service responder to")
                .dest("datastore");

        parser.addArgument("--datastore-modules")
                .type(String.class)
                .help("Comma separated list of YANG modules that will be exposed in datastore")
                .dest("datastore-modules");

        parser.addArgument("--yang-directory")
                .type(String.class)
                .required(true)
                .help("Directory containing YANG modules.")
                .dest("yang-directory");

        parser.addArgument("--rpc")
                .type(String.class)
                .required(false)
                .help("RPC endpoint (partially) implementing test-model YANG")
                .dest("rpc");

        final Options opts = new Options();
        parser.parseArgs(args, opts);
        return opts;
    }
}
