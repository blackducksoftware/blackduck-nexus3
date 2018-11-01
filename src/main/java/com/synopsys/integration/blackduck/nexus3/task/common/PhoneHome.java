package com.synopsys.integration.blackduck.nexus3.task.common;

import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.BlackDuckConnection;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.phonehome.PhoneHomeCallable;
import com.synopsys.integration.phonehome.PhoneHomeRequestBody;
import com.synopsys.integration.phonehome.PhoneHomeService;

@Named
@Singleton
public class PhoneHome {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final BlackDuckConnection blackDuckConnection;

    @Inject
    public PhoneHome(final BlackDuckConnection blackDuckConnection) {
        this.blackDuckConnection = blackDuckConnection;
    }

    public PhoneHomeCallable createPhoneHomeCallable(final String taskName) throws IntegrationException {
        final PhoneHomeRequestBody.Builder phoneHomeRequestBody = new PhoneHomeRequestBody.Builder();
        phoneHomeRequestBody.addToMetaData("task.type", taskName);

        final Version version = FrameworkUtil.getBundle(getClass()).getVersion();
        final String productVersion = version.toString();
        final String artifactId = FrameworkUtil.getBundle(getClass()).getSymbolicName();
        logger.debug("Found {} version {}", artifactId, productVersion);

        final HubServerConfig hubServerConfig = blackDuckConnection.getHubServerConfig();
        final URL blackDuckUrl = hubServerConfig.getHubUrl();
        final HubServicesFactory hubServicesFactory = blackDuckConnection.getHubServicesFactory();
        return hubServicesFactory.createBlackDuckPhoneHomeCallable(blackDuckUrl, artifactId, productVersion, phoneHomeRequestBody);
    }

    public void sendDataHome(final PhoneHomeCallable phoneHomeCallable) throws IntegrationException {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final HubServicesFactory hubServicesFactory = blackDuckConnection.getHubServicesFactory();
        final PhoneHomeService phoneHomeService = hubServicesFactory.createPhoneHomeService(executorService);
        phoneHomeService.phoneHome(phoneHomeCallable);
        executorService.shutdownNow();
    }
}
