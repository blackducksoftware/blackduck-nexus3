package com.synopsys.integration.blackduck.nexus3.mock;

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.blackduck.nexus3.BlackDuckConnection;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.test.TestProperties;
import com.synopsys.integration.test.TestPropertyKey;

public class MockBlackDuckConnection extends BlackDuckConnection {

    public MockBlackDuckConnection() {
        super(null);
    }

    @Override
    public BlackDuckServerConfig getBlackDuckServerConfig() throws IntegrationException {
        final BlackDuckServerConfig blackDuckServerConfig = createHubServerConfig();
        setHubServerConfig(blackDuckServerConfig);
        return createHubServerConfig();
    }

    private BlackDuckServerConfig createHubServerConfig() {
        final TestProperties testProperties = new TestProperties();
        final String blackDuckUrl = testProperties.getProperty(TestPropertyKey.TEST_HUB_SERVER_URL);
        final String apiKey = testProperties.getProperty(TestPropertyKey.TEST_HUB_API_KEY);
        final boolean trustCert = Boolean.parseBoolean(testProperties.getProperty(TestPropertyKey.TEST_TRUST_HTTPS_CERT));
        final int timeout = Integer.parseInt(testProperties.getProperty(TestPropertyKey.TEST_HUB_TIMEOUT));

        final BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = new BlackDuckServerConfigBuilder();
        blackDuckServerConfigBuilder.setUrl(blackDuckUrl);
        blackDuckServerConfigBuilder.setTimeout(timeout);
        blackDuckServerConfigBuilder.setTrustCert(trustCert);
        blackDuckServerConfigBuilder.setApiToken(apiKey);
        return blackDuckServerConfigBuilder.build();
    }
}
