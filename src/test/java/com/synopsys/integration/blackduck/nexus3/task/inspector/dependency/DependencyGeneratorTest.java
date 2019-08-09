package com.synopsys.integration.blackduck.nexus3.task.inspector.dependency;

import java.util.HashMap;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;

import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.bdio.model.dependency.Dependency;

public class DependencyGeneratorTest {

    @Test
    public void findDependencyTest() {
        final DependencyGenerator dependencyGenerator = new DependencyGenerator();

        final String maven = "maven2";
        final Optional<DependencyType> mavenDependency = dependencyGenerator.findDependency(maven);

        Assert.assertTrue(mavenDependency.isPresent());
        Assert.assertEquals(Forge.MAVEN, mavenDependency.get().getForge());

        final String nuget = "nuget";
        final Optional<DependencyType> nugetDependency = dependencyGenerator.findDependency(nuget);

        Assert.assertTrue(nugetDependency.isPresent());
        Assert.assertEquals(Forge.NUGET, nugetDependency.get().getForge());

        final String badName = "badName";
        final Optional<DependencyType> emptyDependency = dependencyGenerator.findDependency(badName);

        Assert.assertFalse(emptyDependency.isPresent());
    }

    @Test
    public void createDependencyTest() {
        final DependencyGenerator dependencyGenerator = new DependencyGenerator();

        final NestedAttributesMap defaultAttributesMap = new NestedAttributesMap(MetadataNodeEntityAdapter.P_ATTRIBUTES, new HashMap<>());

        defaultAttributesMap.child("maven2").set("groupId", "testGroup");
        final Dependency mavenDependency = dependencyGenerator.createDependency(DependencyType.maven, "maven", "Test", defaultAttributesMap);

        final String originId = "testGroup:maven:Test";
        Assert.assertEquals(originId, mavenDependency.externalId.createExternalId());

        final Dependency nugetDependency = dependencyGenerator.createDependency(DependencyType.nuget, "nugetTest", "test1", defaultAttributesMap);

        final String nugetOriginId = "nugetTest/test1";
        Assert.assertEquals(nugetOriginId, nugetDependency.externalId.createExternalId());
    }
}
