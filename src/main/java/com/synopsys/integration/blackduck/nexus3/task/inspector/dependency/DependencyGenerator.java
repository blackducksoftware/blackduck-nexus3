/**
 * blackduck-nexus3
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.nexus3.task.inspector.dependency;

import java.util.Arrays;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Format;

import com.synopsys.integration.hub.bdio.model.Forge;
import com.synopsys.integration.hub.bdio.model.dependency.Dependency;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalId;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalIdFactory;

@Named
@Singleton
public class DependencyGenerator {
    public static final Forge YUM_FORGE = new Forge("/", "/", "@Centos");
    private final ExternalIdFactory externalIdFactory;

    public DependencyGenerator() {
        externalIdFactory = new ExternalIdFactory();
    }

    public Optional<DependencyType> findDependency(final Format repositoryFormat) {
        final String formatName = repositoryFormat.getValue();
        return Arrays.stream(DependencyType.values())
                   .filter(dependencyType -> dependencyType.getRepositoryType().equals(formatName))
                   .findFirst();
    }

    public Dependency createDependency(final DependencyType dependencyType, final String name, final String version, final NestedAttributesMap attributesMap) {
        if (DependencyType.maven == dependencyType) {
            final String group = attributesMap.child("maven2").get("groupId", String.class);
            final ExternalId mavenExternalId = externalIdFactory.createMavenExternalId(group, name, version);
            return new Dependency(name, version, mavenExternalId);
        }

        final ExternalId externalId = externalIdFactory.createNameVersionExternalId(dependencyType.getForge(), name, version);
        return new Dependency(name, version, externalId);
    }

}
