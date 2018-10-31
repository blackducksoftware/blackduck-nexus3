package com.synopsys.integration.blackduck.nexus3.mock;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.configuration.HubServerConfigBuilder;
import com.synopsys.integration.blackduck.nexus3.BlackDuckConnection;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.test.TestProperties;
import com.synopsys.integration.test.TestPropertyKey;

public class MockBlackDuckConnection extends BlackDuckConnection {

    public MockBlackDuckConnection() {
        super(null);
    }

    @Override
    public HubServerConfig getHubServerConfig() throws IntegrationException {
        final HubServerConfig hubServerConfig = createHubServerConfig();
        setHubServerConfig(hubServerConfig);
        return createHubServerConfig();
    }

    private HubServerConfig createHubServerConfig() {
        final TestProperties testProperties = new TestProperties();
        final String blackDuckUrl = testProperties.getProperty(TestPropertyKey.TEST_HUB_SERVER_URL);
        final String apiKey = testProperties.getProperty(TestPropertyKey.TEST_HUB_API_KEY);
        final boolean trustCert = Boolean.parseBoolean(testProperties.getProperty(TestPropertyKey.TEST_TRUST_HTTPS_CERT));
        final int timeout = Integer.parseInt(testProperties.getProperty(TestPropertyKey.TEST_HUB_TIMEOUT));

        final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
        hubServerConfigBuilder.setUrl(blackDuckUrl);
        hubServerConfigBuilder.setTimeout(timeout);
        hubServerConfigBuilder.setTrustCert(trustCert);
        hubServerConfigBuilder.setApiToken(apiKey);
        return hubServerConfigBuilder.build();
    }
}
