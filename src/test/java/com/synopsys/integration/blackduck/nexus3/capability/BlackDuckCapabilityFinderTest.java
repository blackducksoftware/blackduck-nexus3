package com.synopsys.integration.blackduck.nexus3.capability;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonatype.nexus.capability.Capability;
import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilitySupport;

public class BlackDuckCapabilityFinderTest {

    @Test
    public void retrieveBadCapabilityConfigurationTest() {

    }

    @Test
    public void retrieveFakeCapabilityConfigurationTest() {
        final CapabilityRegistry capabilityRegistry = Mockito.mock(CapabilityRegistry.class);
        final TestCapabilityReference testCapabilityReference = new TestCapabilityReference();
        final Collection<? extends CapabilityReference> references = Arrays.asList(testCapabilityReference);
        Mockito.doReturn(references).when(capabilityRegistry).getAll();

        final BlackDuckCapabilityFinder blackDuckCapabilityFinder = new BlackDuckCapabilityFinder(capabilityRegistry);
        final BlackDuckCapabilityConfiguration blackDuckCapabilityConfiguration = blackDuckCapabilityFinder.retrieveBlackDuckCapabilityConfiguration();

        Assert.assertNull(blackDuckCapabilityConfiguration);
    }

    @Test
    public void retrieveNullCapabilityConfigurationTest() {
        final CapabilityRegistry capabilityRegistry = Mockito.mock(CapabilityRegistry.class);
        Mockito.when(capabilityRegistry.getAll()).thenReturn(Arrays.asList());

        final BlackDuckCapabilityFinder blackDuckCapabilityFinder = new BlackDuckCapabilityFinder(capabilityRegistry);
        final BlackDuckCapabilityConfiguration blackDuckCapabilityConfiguration = blackDuckCapabilityFinder.retrieveBlackDuckCapabilityConfiguration();

        Assert.assertNull(blackDuckCapabilityConfiguration);
    }

    class TestCapabilityReference implements CapabilityReference {
        @Override
        public CapabilityContext context() {
            return null;
        }

        @Override
        public Capability capability() {
            return new TestCapability();
        }

        @Override
        public <T extends Capability> T capabilityAs(final Class<T> aClass) {
            return (T) capability();
        }
    }

    class TestCapability extends CapabilitySupport<Map<String, String>> {

        @Override
        protected Map<String, String> createConfig(final Map map) throws Exception {
            return map;
        }

    }
}
