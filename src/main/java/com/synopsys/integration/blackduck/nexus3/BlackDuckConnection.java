/**
 * blackduck-nexus3
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.nexus3;

import java.io.IOException;

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
    private BlackduckRestConnection restConnection;

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
            final BlackduckRestConnection restConnection = getBlackDuckRestConnection(intLogger);
            hubServicesFactory = new HubServicesFactory(HubServicesFactory.createDefaultGson(), HubServicesFactory.createDefaultJsonParser(), restConnection, intLogger);
        }

        return hubServicesFactory;
    }

    public BlackduckRestConnection getBlackDuckRestConnection(final IntLogger intLogger) throws IntegrationException {
        if (needsUpdate || restConnection == null) {
            restConnection = getHubServerConfig().createRestConnection(intLogger);
        }

        return restConnection;
    }

    public void closeBlackDuckRestConnection() throws IOException {
        if (restConnection != null) {
            logger.info("Closing connection to BlackDuck.");
            restConnection.close();
            restConnection = null;
            hubServicesFactory = null;
            needsUpdate = true;
        }
    }
}
