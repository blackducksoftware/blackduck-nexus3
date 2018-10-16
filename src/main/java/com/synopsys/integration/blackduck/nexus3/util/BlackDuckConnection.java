package com.synopsys.integration.blackduck.nexus3.util;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.capability.BlackDuckCapabilityConfiguration;
import com.synopsys.integration.blackduck.nexus3.capability.BlackDuckCapabilityFinder;
import com.synopsys.integration.blackduck.rest.BlackduckRestConnection;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

@Named
@Singleton
public class BlackDuckConnection {
    private final BlackDuckCapabilityFinder blackDuckCapabilityFinder;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private boolean needsUpdate;
    private HubServerConfig hubServerConfig;
    private HubServicesFactory hubServicesFactory;

    @Inject
    public BlackDuckConnection(final BlackDuckCapabilityFinder blackDuckCapabilityFinder) {
        this.blackDuckCapabilityFinder = blackDuckCapabilityFinder;
        markForUpdate();
    }

    public void updateHubServerConfig() throws IntegrationException {
        final BlackDuckCapabilityConfiguration blackDuckCapabilityConfiguration = blackDuckCapabilityFinder.retrieveBlackDuckCapabilityConfiguration();
        if (blackDuckCapabilityConfiguration == null) {
            throw new IntegrationException("BlackDuck server configuration not found.");
        }
        final HubServerConfig updatedHubServerConfig = blackDuckCapabilityConfiguration.createBlackDuckServerConfig();
        setHubServerConfig(updatedHubServerConfig);
    }

    public HubServerConfig getHubServerConfig() throws IntegrationException {
        if (needsUpdate || hubServerConfig == null) {
            updateHubServerConfig();
        }

        return hubServerConfig;
    }

    public void setHubServerConfig(final HubServerConfig hubServerConfig) {
        this.hubServerConfig = hubServerConfig;
        hubServicesFactory = null;
        needsUpdate = false;
    }

    public void markForUpdate() {
        needsUpdate = true;
    }

    public HubServicesFactory getHubServicesFactory() throws IntegrationException {
        if (needsUpdate || hubServicesFactory == null) {
            logger.debug("Getting updated hubServicesFactory");
            final IntLogger intLogger = new Slf4jIntLogger(logger);
            final BlackduckRestConnection restConnection = getHubServerConfig().createRestConnection(intLogger);
            hubServicesFactory = new HubServicesFactory(HubServicesFactory.createDefaultGson(), HubServicesFactory.createDefaultJsonParser(), restConnection, intLogger);
        }

        return hubServicesFactory;
    }
}
