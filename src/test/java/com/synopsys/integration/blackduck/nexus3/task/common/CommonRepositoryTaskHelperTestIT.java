package com.synopsys.integration.blackduck.nexus3.task.common;

import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.mock.MockBlackDuckConnection;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.test.TestProperties;
import com.synopsys.integration.test.TestPropertyKey;

public class CommonRepositoryTaskHelperTestIT {

    @Test
    public void getHubServerConfigTest() throws IntegrationException {
        final MockBlackDuckConnection mockBlackDuckConnection = new MockBlackDuckConnection();
        final CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(null, null, mockBlackDuckConnection);

        final HubServerConfig hubServerConfig = commonRepositoryTaskHelper.getHubServerConfig();
        Assert.assertNotNull(hubServerConfig);

        final URL blackDuckUrl = hubServerConfig.getHubUrl();
        Assert.assertNotNull(blackDuckUrl);

        final TestProperties testProperties = new TestProperties();
        final String storedUrl = testProperties.getProperty(TestPropertyKey.TEST_HUB_SERVER_URL);
        Assert.assertEquals(blackDuckUrl.toExternalForm(), storedUrl);
    }

    @Test
    public void getHubServicesFactoryTest() throws IntegrationException {
        final MockBlackDuckConnection mockBlackDuckConnection = new MockBlackDuckConnection();
        final CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(null, null, mockBlackDuckConnection);

        final HubServicesFactory hubServicesFactory = commonRepositoryTaskHelper.getHubServicesFactory();
        Assert.assertNotNull(hubServicesFactory);
    }
}
