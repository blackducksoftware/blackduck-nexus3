package com.synopsys.integration.blackduck.nexus3.task.inspector.dependency;

import com.synopsys.integration.hub.bdio.model.Forge;

public enum DependencyType {
    bower(Forge.BOWER, "bower"),
    maven(Forge.MAVEN, "maven2"),
    npm(Forge.NPM, "npm"),
    nuget(Forge.NUGET, "nuget"),
    pypi(Forge.PYPI, "pypi"),
    rubygems(Forge.RUBYGEMS, "rubygems"),
    yum(DependencyGenerator.YUM_FORGE, "yum");

    private final Forge dependencyForge;
    private final String repositoryType;

    DependencyType(final Forge forge, final String repositoryType) {
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
