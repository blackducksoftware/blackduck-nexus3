package com.synopsys.integration.blackduck.nexus3.task.common;

import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.nexus3.mock.MockBlackDuckConnection;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.test.TestProperties;
import com.synopsys.integration.test.TestPropertyKey;

public class CommonRepositoryTaskHelperTestIT {

    @Test
    public void getHubServerConfigTest() throws IntegrationException {
        final MockBlackDuckConnection mockBlackDuckConnection = new MockBlackDuckConnection();
        final CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(null, null, mockBlackDuckConnection);

        final BlackDuckServerConfig blackDuckServerConfig = commonRepositoryTaskHelper.getBlackDuckServerConfig();
        Assert.assertNotNull(blackDuckServerConfig);

        final URL blackDuckUrl = blackDuckServerConfig.getBlackDuckUrl();
        Assert.assertNotNull(blackDuckUrl);

        final TestProperties testProperties = new TestProperties();
        final String storedUrl = testProperties.getProperty(TestPropertyKey.TEST_HUB_SERVER_URL);
        Assert.assertEquals(blackDuckUrl.toExternalForm(), storedUrl);
    }

    @Test
    public void getHubServicesFactoryTest() throws IntegrationException {
        final MockBlackDuckConnection mockBlackDuckConnection = new MockBlackDuckConnection();
        final CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(null, null, mockBlackDuckConnection);

        final BlackDuckServicesFactory blackDuckServicesFactory = commonRepositoryTaskHelper.getBlackDuckServicesFactory();
        Assert.assertNotNull(blackDuckServicesFactory);
    }
}
