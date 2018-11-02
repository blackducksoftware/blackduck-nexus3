package com.synopsys.integration.blackduck.nexus3.capability;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.formfields.FormField;

public class BlackDuckCapabilityDescriptorTest {

    @Test
    public void typeTest() {
        final BlackDuckCapabilityDescriptor blackDuckCapabilityDescriptor = new BlackDuckCapabilityDescriptor();
        final CapabilityType capabilityType = blackDuckCapabilityDescriptor.type();

        Assert.assertEquals(BlackDuckCapabilityDescriptor.CAPABILITY_ID, capabilityType.toString());
    }

    @Test
    public void nameTest() {
        final BlackDuckCapabilityDescriptor blackDuckCapabilityDescriptor = new BlackDuckCapabilityDescriptor();
        final String name = blackDuckCapabilityDescriptor.name();

        Assert.assertEquals(BlackDuckCapabilityDescriptor.CAPABILITY_NAME, name);
    }

    @Test
    public void aboutTest() {
        final BlackDuckCapabilityDescriptor blackDuckCapabilityDescriptor = new BlackDuckCapabilityDescriptor();
        final String about = blackDuckCapabilityDescriptor.about();

        Assert.assertEquals(BlackDuckCapabilityDescriptor.CAPABILITY_DESCRIPTION, about);
    }

    @Test
    public void formFieldsTest() {
        final BlackDuckCapabilityDescriptor blackDuckCapabilityDescriptor = new BlackDuckCapabilityDescriptor();
        final List<FormField> formFields = blackDuckCapabilityDescriptor.formFields();

        Assert.assertEquals(8, formFields.size());
    }

    @Test
    public void validateConfigTest() {
        final BlackDuckCapabilityDescriptor blackDuckCapabilityDescriptor = new BlackDuckCapabilityDescriptor() {

            @Override
            protected void validate(final Object value, final Class<?>... groups) {
                return;
            }
        };

        final Map<String, String> capabilitySettings = new HashMap<>();
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_URL.getKey(), "http://google.com");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_TIMEOUT.getKey(), "300");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_API_KEY.getKey(), "apiKey");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_HOST.getKey(), "proxyHost.com");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PORT.getKey(), "80");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_USERNAME.getKey(), "proxyUsername");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PASSWORD.getKey(), "proxyPassword");

        try {
            blackDuckCapabilityDescriptor.validateConfig(capabilitySettings, CapabilityDescriptor.ValidationMode.LOAD);
        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

    }
}
