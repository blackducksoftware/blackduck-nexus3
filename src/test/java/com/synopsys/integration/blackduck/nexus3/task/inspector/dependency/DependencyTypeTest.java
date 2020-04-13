package com.synopsys.integration.blackduck.nexus3.task.inspector.dependency;

import org.junit.Assert;
import org.junit.Test;

import com.synopsys.integration.bdio.model.Forge;

public class DependencyTypeTest {

    @Test
    public void enforcedTypeNames() {
        Assert.assertEquals("maven2", DependencyType.MAVEN.getRepositoryType());
        Assert.assertEquals(Forge.MAVEN, DependencyType.MAVEN.getForge());

        Assert.assertEquals("bower", DependencyType.BOWER.getRepositoryType());
        Assert.assertEquals(Forge.BOWER, DependencyType.BOWER.getForge());

        Assert.assertEquals("npm", DependencyType.NPM.getRepositoryType());
        Assert.assertEquals(Forge.NPMJS, DependencyType.NPM.getForge());

        Assert.assertEquals("nuget", DependencyType.NUGET.getRepositoryType());
        Assert.assertEquals(Forge.NUGET, DependencyType.NUGET.getForge());

        Assert.assertEquals("pypi", DependencyType.PYPI.getRepositoryType());
        Assert.assertEquals(Forge.PYPI, DependencyType.PYPI.getForge());

        Assert.assertEquals("rubygems", DependencyType.RUBYGEMS.getRepositoryType());
        Assert.assertEquals(Forge.RUBYGEMS, DependencyType.RUBYGEMS.getForge());

        Assert.assertEquals("yum", DependencyType.YUM.getRepositoryType());
        Assert.assertEquals(DependencyGenerator.YUM_FORGE, DependencyType.YUM.getForge());
    }
}
