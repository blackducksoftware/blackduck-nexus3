package com.synopsys.integration.blackduck.nexus3.task.common;

import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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
    private final BlackDuckConnection blackDuckConnection;

    @Inject
    public PhoneHome(final BlackDuckConnection blackDuckConnection) {
        this.blackDuckConnection = blackDuckConnection;
    }

    public PhoneHomeCallable createPhoneHomeCallable(final String taskName) throws IntegrationException {
        final PhoneHomeRequestBody.Builder phoneHomeRequestBody = new PhoneHomeRequestBody.Builder();
        // TODO find a way to auto update this property
        final String productVersion = "1.0.0";
        final String artifactId = "blackduck-nexus3";
        phoneHomeRequestBody.addToMetaData("task.type", taskName);

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
