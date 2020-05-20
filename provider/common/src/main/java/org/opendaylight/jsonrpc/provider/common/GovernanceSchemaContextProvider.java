/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Queues;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.model.ModuleInfo;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.model.SchemaContextProvider;
import org.opendaylight.jsonrpc.model.StringYangTextSchemaSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContextProvider;
import org.opendaylight.yangtools.yang.model.api.ModuleImport;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.rfc7950.reactor.RFC7950Reactors;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.ASTSchemaSource;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.TextToASTTransformer;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangStatementStreamSource;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor.BuildAction;
import org.opendaylight.yangtools.yang.xpath.api.YangXPathParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link EffectiveModelContextProvider} which uses {@link RemoteGovernance} to obtain models
 * from.Implementation is fail-fast, so any missing model will cause error. Models dependencies are resolved
 * recursively first.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public class GovernanceSchemaContextProvider implements SchemaContextProvider {
    private static final Logger LOG = LoggerFactory.getLogger(GovernanceSchemaContextProvider.class);
    private final RemoteGovernance governance;
    private static final Duration CACHE_TTL = Duration.ofMinutes(10L);
    private final YangXPathParserFactory xpathParserFactory;
    // cache to speed-up module import lookups
    private final LoadingCache<ModuleInfo, Set<ModuleImport>> moduleImportCache = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_TTL)
            .build(new CacheLoader<ModuleInfo, Set<ModuleImport>>() {
                @Override
                public Set<ModuleImport> load(ModuleInfo key) throws Exception {
                    LOG.trace("Resolving imports of module '{}'", key);
                    final String content = sourceCache.getUnchecked(key);
                    final ASTSchemaSource schemaSource = TextToASTTransformer
                            .transformText(new StringYangTextSchemaSource(key.getModule(), content));
                    return schemaSource.getDependencyInformation().getDependencies();
                }
            });

    //cache to speed-up module source fetch
    private final LoadingCache<ModuleInfo, String> sourceCache = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_TTL)
            .build(new CacheLoader<ModuleInfo, String>() {
                @Override
                public String load(ModuleInfo key) throws Exception {
                    LOG.trace("Fetching source of module '{}'", key.getModule());
                    return Optional.ofNullable(governance.source(key.getModule()))
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Module not found in governance : " + key.getModule()));
                }
            });

    public GovernanceSchemaContextProvider(@NonNull final RemoteGovernance governance,
            @NonNull final YangXPathParserFactory xpathParserFactory) {
        this.governance = Objects.requireNonNull(governance);
        this.xpathParserFactory = Objects.requireNonNull(xpathParserFactory);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public EffectiveModelContext createSchemaContext(Peer peer) {
        try {
            return createInternal(peer);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to build EffectiveModelContext", e);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private EffectiveModelContext createInternal(Peer peer) throws ReactorException {
        final BuildAction reactor = RFC7950Reactors.defaultReactorBuilder(xpathParserFactory).build().newBuild();
        final Deque<ModuleInfo> toResolve = Queues.newArrayDeque();
        try {
            Optional.ofNullable(peer.getModules())
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(Objects::nonNull)
                    .forEach(mod -> {
                        LOG.debug("Resolving dependencies of '{}'", mod.getValue());
                        toResolve.addAll(governance.depends(mod.getValue(), null));
                    });
        } catch (Exception e) {
            LOG.warn("Governance failed to provide dependencies, will attempt to resolve it locally", e);
            final Set<ModuleInfo> resolved = new HashSet<>();
            toResolve.clear();

            // seed model set with what user gave to us
            toResolve.addAll(peer.getModules().stream().map(yi -> {
                if (yi.getValue().indexOf('@') != -1) {
                    final String[] parts = yi.getValue().split("@");
                    return new ModuleInfo(parts[0], parts[1]);
                }
                return new ModuleInfo(yi.getValue(), null);
            }).collect(Collectors.toList()));
            // resolve remaining until queue is empty
            while (!toResolve.isEmpty()) {
                final ModuleInfo mi = toResolve.pop();
                final Set<ModuleInfo> imports = moduleImportCache.getUnchecked(mi)
                        .stream()
                        .map(imp -> new ModuleInfo(imp.getModuleName(), null))
                        .filter(m -> !resolved.contains(m))
                        .filter(m -> !toResolve.contains(m))
                        .collect(Collectors.toSet());
                resolved.add(mi);
                toResolve.addAll(imports);
                LOG.trace("Remaining to resolve : {}", toResolve);
            }
            toResolve.addAll(resolved);
        }
        LOG.trace("Assembling schema from following modules : {}", toResolve);
        toResolve.stream()
                .distinct()
                .forEach(m -> addSourceToReactor(reactor, m.getModule(), sourceCache.getUnchecked(m)));
        return reactor.buildEffective();
    }

    private void addSourceToReactor(BuildAction reactor, String name, String yangSource) {
        try {
            reactor.addSource(YangStatementStreamSource.create(YangTextSchemaSource.delegateForByteSource(
                    name + ".yang", ByteSource.wrap(yangSource.getBytes(StandardCharsets.UTF_8)))));
        } catch (YangSyntaxErrorException | IOException e) {
            throw new IllegalStateException("Unable to add source of '" + name + "' into reactor", e);
        }
    }
}
