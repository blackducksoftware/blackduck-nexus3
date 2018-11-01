package com.synopsys.integration.blackduck.nexus3.capability;

import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.capability.CapabilityContext;

import com.synopsys.integration.blackduck.nexus3.BlackDuckConnection;

public class BlackDuckCapabilityTest extends TestSupport {

    @Test
    public void createConfigTest() {
        final BlackDuckCapability blackDuckCapability = new BlackDuckCapability(null);
        BlackDuckCapabilityConfiguration blackDuckCapabilityConfiguration = blackDuckCapability.createConfig(Collections.emptyMap());

        Assert.assertNotNull(blackDuckCapabilityConfiguration);
    }

    @Test
    public void isPasswordPropertyTest() {
        final BlackDuckCapability blackDuckCapability = new BlackDuckCapability(null);
        final boolean random = blackDuckCapability.isPasswordProperty("Nothing");
        final boolean username = blackDuckCapability.isPasswordProperty(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_USERNAME.getKey());
        final boolean password = blackDuckCapability.isPasswordProperty(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PASSWORD.getKey());
        final boolean apiKey = blackDuckCapability.isPasswordProperty(BlackDuckCapabilityConfigKeys.BLACKDUCK_API_KEY.getKey());

        Assert.assertFalse(random);
        Assert.assertFalse(username);
        Assert.assertTrue(password);
        Assert.assertTrue(apiKey);
    }

    @Test
    public void configureTest() throws Exception {
        final BlackDuckConnection blackDuckConnection = Mockito.mock(BlackDuckConnection.class);
        final BlackDuckCapability blackDuckCapability = new BlackDuckCapability(blackDuckConnection) {
            @Override
            protected BlackDuckCapabilityConfiguration createConfig(final Map<String, String> properties) {
                return new BlackDuckCapabilityConfiguration(Collections.emptyMap());
            }

            @Override
            protected CapabilityContext context() {
                final CapabilityContext capabilityContext = Mockito.mock(CapabilityContext.class);
                Mockito.when(capabilityContext.properties()).thenReturn(Collections.emptyMap());
                return capabilityContext;
            }
        };

        blackDuckCapability.onCreate();
        blackDuckCapability.onUpdate();

        Mockito.verify(blackDuckConnection, Mockito.times(2)).markForUpdate();
    }
}
