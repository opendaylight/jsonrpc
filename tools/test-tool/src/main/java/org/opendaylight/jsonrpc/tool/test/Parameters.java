/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.tool.test;

import java.nio.file.Path;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.inf.ArgumentParser;

class Parameters {
    @Arg(dest = "governance")
    private String governance;

    @Arg(dest = "yang-directory")
    private Path yangDirectory;

    @Arg(dest = "datastore")
    private String datastore;

    @Arg(dest = "datastore-modules")
    private String datastoreModules;

    static ArgumentParser createArgParser() {
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

        return parser;
    }
}
