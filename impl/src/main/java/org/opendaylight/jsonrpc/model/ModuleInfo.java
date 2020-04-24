/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import java.beans.ConstructorProperties;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO to hold very basic information about YANG module.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jan 18, 2019
 */
public final class ModuleInfo {
    private String module;
    private String revision;

    @ConstructorProperties({ "module", "revision" })
    public ModuleInfo(@NonNull String module, @Nullable String revision) {
        this.module = module;
        this.revision = revision;
    }

    public String getModule() {
        return module;
    }

    public String getRevision() {
        return revision;
    }

    @Override
    public int hashCode() {
        return Objects.hash(module, revision);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ModuleInfo other = (ModuleInfo) obj;
        return Objects.equals(other.module, module) && Objects.equals(other.revision, revision);
    }

    @Override
    public String toString() {
        return "ModuleInfo [module=" + module + ", revision=" + revision + "]";
    }
}
