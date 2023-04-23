/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.tool.test;

import static org.opendaylight.yangtools.yang.parser.rfc7950.repo.TextToIRTransformer.transformText;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.opendaylight.jsonrpc.bus.messagelib.ResponderSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.model.ModuleInfo;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.model.StoreOperationArgument;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.repo.api.YangIRSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangModelDependencyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GovernanceImpl implements RemoteGovernance {
    private static final Logger LOG = LoggerFactory.getLogger(GovernanceImpl.class);
    private static final Set<YangModuleInfo> BUNDLED_MODULES = BindingReflections.loadModuleInfos();
    private static final Pattern YANG_MODULE_RE = Pattern
            .compile("(?<name>[a-zA-Z_]?[\\w.-]+)(?<revision>@\\d{4}-\\d{2}-\\d{2})?\\.yang");

    private final LoadingCache<Path, Set<ModuleInfo>> yangFileImportCache = CacheBuilder.newBuilder()
            .build(new CacheLoader<Path, Set<ModuleInfo>>() {
                @Override
                public Set<ModuleInfo> load(Path file) throws Exception {
                    final YangIRSchemaSource irSource = transformText(YangTextSchemaSource.forPath(file));
                    return YangModelDependencyInfo.forIR(irSource)
                            .getDependencies()
                            .stream()
                            .map(m -> new ModuleInfo(m.getModuleName().getLocalName(), null))
                            .collect(Collectors.toSet());
                }
            });

    private ResponderSession session;
    private final Path yangDir;

    GovernanceImpl(TransportFactory transportFactory, String endpoint, Path yangDir) throws URISyntaxException {
        if (endpoint != null) {
            session = transportFactory.endpointBuilder().responder().create(endpoint, this);
        }
        this.yangDir = yangDir;
    }

    @Override
    public String governance(StoreOperationArgument arg) {
        LOG.info("[governance] : {}", arg);
        // NOOP in this impl.
        return null;
    }

    @Override
    public String source(ModuleInfo arg) {
        LOG.info("[source] : {}", arg);
        try {
            final Optional<YangModuleInfo> bundled = findBundledModule(arg);
            if (bundled.isPresent()) {
                return bundled.orElseThrow().getYangTextCharSource().read();
            }
            final Optional<Path> opt = findYangFile(arg);
            if (opt.isPresent()) {
                return Files.readString(opt.orElseThrow());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }

    @Override
    public List<ModuleInfo> depends(ModuleInfo moduleInfo) {
        LOG.info("[depends] : {}", moduleInfo);

        final Set<ModuleInfo> resolved = new HashSet<>();
        final Deque<ModuleInfo> toResolve = new LinkedList<>();
        toResolve.add(moduleInfo);
        while (!toResolve.isEmpty()) {
            LOG.debug("Resolved : {}", resolved);
            LOG.trace("Remaining to resolve : {}", toResolve);
            final ModuleInfo current = toResolve.pop();
            resolved.add(current);
            final Set<ModuleInfo> currentImports = dependsInternal(current).stream()
                    .filter(m -> !resolved.contains(m))
                    .filter(m -> !toResolve.contains(m))
                    .collect(Collectors.toSet());
            toResolve.addAll(currentImports);
        }
        return new ArrayList<>(resolved);
    }

    private static Set<ModuleInfo> parseDependencies(YangModuleInfo ymi) {
        return ymi.getImportedModules()
                .stream()
                .map(mi -> new ModuleInfo(mi.getName().getLocalName(), null))
                .collect(Collectors.toSet());
    }

    private Optional<Path> findYangFile(ModuleInfo mi) throws IOException {
        final YangFileSearchResult result = new YangFileSearchResult(mi);
        Files.walkFileTree(yangDir, result);
        return result.getResult();
    }

    private static Optional<YangModuleInfo> findBundledModule(ModuleInfo mi) {
        return BUNDLED_MODULES.stream().filter(ymi -> ymi.getName().getLocalName().equals(mi.getModule())).findFirst();
    }

    private static class YangFileSearchResult extends SimpleFileVisitor<Path> {
        private final ModuleInfo moduleInfo;
        private Optional<Path> result = Optional.empty();

        YangFileSearchResult(ModuleInfo moduleInfo) {
            this.moduleInfo = moduleInfo;
        }

        @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
                justification = "Path#getFileName() won't return NULL")
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            LOG.trace("Considering file {}", file);
            final Matcher matcher = YANG_MODULE_RE.matcher(file.getFileName().toString());
            if (matcher.matches() && moduleInfo.getModule().equals(matcher.group("name"))) {
                result = Optional.of(file.toAbsolutePath());
                LOG.info("Found {}", file);
                return FileVisitResult.TERMINATE;
            }
            return super.visitFile(file, attrs);
        }

        Optional<Path> getResult() {
            return result;
        }
    }

    private Set<ModuleInfo> dependsInternal(ModuleInfo module) {
        final Optional<YangModuleInfo> bundledOpt = findBundledModule(module);
        if (bundledOpt.isPresent()) {
            return parseDependencies(bundledOpt.orElseThrow());
        }
        try {
            final Optional<Path> optFile = findYangFile(module);
            if (optFile.isPresent()) {
                return yangFileImportCache.getUnchecked(optFile.orElseThrow());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalArgumentException("Module not found : " + module);
    }

    @Override
    public void close() {
        if (session != null) {
            session.close();
        }
    }

    @Override
    public String toString() {
        return "GovernanceImpl [session=" + session + ", yangDir=" + yangDir + "]";
    }
}
