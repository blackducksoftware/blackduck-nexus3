package com.synopsys.integration.blackduck.nexus3.task.inspector.dependency;

import org.junit.Assert;
import org.junit.Test;

import com.synopsys.integration.hub.bdio.model.Forge;

public class DependencyTypeTest {

    @Test
    public void enforcedTypeNames() {
        Assert.assertEquals("maven2", DependencyType.maven.getRepositoryType());
        Assert.assertEquals(Forge.MAVEN, DependencyType.maven.getForge());

        Assert.assertEquals("bower", DependencyType.bower.getRepositoryType());
        Assert.assertEquals(Forge.BOWER, DependencyType.bower.getForge());

        Assert.assertEquals("npm", DependencyType.npm.getRepositoryType());
        Assert.assertEquals(Forge.NPM, DependencyType.npm.getForge());

        Assert.assertEquals("nuget", DependencyType.nuget.getRepositoryType());
        Assert.assertEquals(Forge.NUGET, DependencyType.nuget.getForge());

        Assert.assertEquals("pypi", DependencyType.pypi.getRepositoryType());
        Assert.assertEquals(Forge.PYPI, DependencyType.pypi.getForge());

        Assert.assertEquals("rubygems", DependencyType.rubygems.getRepositoryType());
        Assert.assertEquals(Forge.RUBYGEMS, DependencyType.rubygems.getForge());

        Assert.assertEquals("yum", DependencyType.yum.getRepositoryType());
        Assert.assertEquals(DependencyGenerator.YUM_FORGE, DependencyType.yum.getForge());
    }
}
