package com.synopsys.integration.blackduck.nexus3.capability;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;

public class BlackDuckCapabilityFinderTest {

    @Test
    public void retrieveNullCapabilityConfigurationTest() {
        CapabilityRegistry capabilityRegistry = Mockito.mock(CapabilityRegistry.class);
        Mockito.when(capabilityRegistry.get(Mockito.<com.google.common.base.Predicate<CapabilityReference>>anyObject())).thenReturn(Arrays.asList());

        BlackDuckCapabilityFinder blackDuckCapabilityFinder = new BlackDuckCapabilityFinder(capabilityRegistry);
        BlackDuckCapabilityConfiguration blackDuckCapabilityConfiguration = blackDuckCapabilityFinder.retrieveBlackDuckCapabilityConfiguration();

        Assert.assertNull(blackDuckCapabilityConfiguration);
    }

}
