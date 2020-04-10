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

import com.synopsys.integration.bdio.model.Forge;

public enum DependencyType {
    BOWER(Forge.BOWER, "bower"),
    MAVEN(Forge.MAVEN, "maven2"),
    NPM(Forge.NPMJS, "npm"),
    NUGET(Forge.NUGET, "nuget"),
    PYPI(Forge.PYPI, "pypi"),
    RUBYGEMS(Forge.RUBYGEMS, "rubygems"),
    YUM(DependencyGenerator.YUM_FORGE, "yum");

    private final Forge dependencyForge;
    private final String repositoryType;

    DependencyType(Forge forge, String repositoryType) {
        dependencyForge = forge;
        this.repositoryType = repositoryType;
    }

    public Forge getForge() {
        return dependencyForge;
    }

    public String getRepositoryType() {
        return repositoryType;
    }
}
