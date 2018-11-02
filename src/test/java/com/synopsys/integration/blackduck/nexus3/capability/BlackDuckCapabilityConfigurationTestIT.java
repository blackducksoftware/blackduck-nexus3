package com.synopsys.integration.blackduck.nexus3.capability;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.test.TestProperties;
import com.synopsys.integration.test.TestPropertyKey;

public class BlackDuckCapabilityConfigurationTestIT {

    @Test
    public void createBlackDuckServerConfigTest() {
        final Map<String, String> capabilitySettings = new HashMap<>();
        final TestProperties testProperties = new TestProperties();

        final String blackDuckUrl = testProperties.getProperty(TestPropertyKey.TEST_HUB_SERVER_URL);
        final String blackDuckApiKey = testProperties.getProperty(TestPropertyKey.TEST_HUB_API_KEY);

        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_URL.getKey(), blackDuckUrl);
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_TIMEOUT.getKey(), "300");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_API_KEY.getKey(), blackDuckApiKey);
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_TRUST_CERT.getKey(), "true");

        final BlackDuckCapabilityConfiguration blackDuckCapabilityConfiguration = new BlackDuckCapabilityConfiguration(capabilitySettings);
        final HubServerConfig hubServerConfig = blackDuckCapabilityConfiguration.createBlackDuckServerConfig();

        Assert.assertTrue(hubServerConfig.usingApiToken());
        Assert.assertEquals(blackDuckUrl, hubServerConfig.getHubUrl().toString());
    }
}
