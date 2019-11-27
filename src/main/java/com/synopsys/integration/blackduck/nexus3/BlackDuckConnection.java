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

import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.nexus3.capability.BlackDuckCapabilityConfiguration;
import com.synopsys.integration.blackduck.nexus3.capability.BlackDuckCapabilityFinder;
import com.synopsys.integration.blackduck.rest.BlackDuckHttpClient;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.IntEnvironmentVariables;

@Named
@Singleton
public class BlackDuckConnection {
    private final BlackDuckCapabilityFinder blackDuckCapabilityFinder;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private boolean needsUpdate;
    private BlackDuckServerConfig blackDuckServerConfig;
    private BlackDuckServicesFactory blackDuckServicesFactory;

    @Inject
    public BlackDuckConnection(final BlackDuckCapabilityFinder blackDuckCapabilityFinder) {
        this.blackDuckCapabilityFinder = blackDuckCapabilityFinder;
        markForUpdate();
    }

    public void updateHubServerConfig() throws IntegrationException {
        final BlackDuckCapabilityConfiguration blackDuckCapabilityConfiguration = blackDuckCapabilityFinder.retrieveBlackDuckCapabilityConfiguration();
        if (blackDuckCapabilityConfiguration == null) {
            throw new IntegrationException("Black Duck server configuration not found.");
        }
        final BlackDuckServerConfig updatedBlackDuckServerConfig = blackDuckCapabilityConfiguration.createBlackDuckServerConfig();
        setHubServerConfig(updatedBlackDuckServerConfig);
    }

    public BlackDuckServerConfig getBlackDuckServerConfig() throws IntegrationException {
        if (needsUpdate || blackDuckServerConfig == null) {
            updateHubServerConfig();
        }

        return blackDuckServerConfig;
    }

    public void setHubServerConfig(final BlackDuckServerConfig blackDuckServerConfig) {
        this.blackDuckServerConfig = blackDuckServerConfig;
        blackDuckServicesFactory = null;
        needsUpdate = false;
    }

    public void markForUpdate() {
        needsUpdate = true;
    }

    public BlackDuckServicesFactory getBlackDuckServicesFactory() throws IntegrationException {
        if (needsUpdate || blackDuckServicesFactory == null) {
            logger.debug("Getting updated blackDuckServicesFactory");
            final IntLogger intLogger = new Slf4jIntLogger(logger);
            if(blackDuckServerConfig == null){
                throw new IntegrationException("The Black Duck configuration is null. Please check that the Black Duck capability has been configured.");
            }
            blackDuckServicesFactory = blackDuckServerConfig.createBlackDuckServicesFactory(intLogger);
        }

        return blackDuckServicesFactory;
    }

}
