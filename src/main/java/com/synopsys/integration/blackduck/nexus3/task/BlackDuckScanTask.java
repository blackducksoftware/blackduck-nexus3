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
package com.synopsys.integration.blackduck.nexus3.task;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilitySupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionDistributionType;
import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionPhaseType;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.capability.HubCapability;
import com.synopsys.integration.blackduck.nexus3.capability.HubCapabilityConfiguration;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.ProjectRequestBuilder;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.Slf4jIntLogger;

@Named
public class BlackDuckScanTask extends RepositoryTaskSupport {
    private static final String BLACKDUCK_CATEGORY = "Black Duck";
    private final Logger logger = createLogger();

    private final QueryManager queryManager;
    private final CapabilityRegistry capabilityRegistry;

    @Inject
    public BlackDuckScanTask(final QueryManager queryManager, final CapabilityRegistry capabilityRegistry) {
        this.queryManager = queryManager;
        this.capabilityRegistry = capabilityRegistry;
    }

    @Override
    public String getMessage() {
        return "BlackDuck scanning repository " + getRepositoryField();
    }

    @Override
    protected boolean appliesTo(final Repository repository) {
        final String repositoryName = getRepositoryField();
        return repository.getName().equals(repositoryName);
    }

    @Override
    protected void execute(final Repository repository) {
        final HubServerConfig hubServerConfig = getHubServerConfig();
        //        final Scanner blackduckScanner = new Scanner(hubServerConfig, );
        logger.info("Found repository: " + repository.getName());
        final Iterable<Asset> foundAssets = queryManager.findAssetsInRepository(repository);
        int version = 1;
        for (final Asset asset : foundAssets) {
            if (shouldScanAsset(asset)) {
                logger.info("Scanning item: " + asset.name());

                final Slf4jIntLogger intLogger = new Slf4jIntLogger(logger);
                final HubServicesFactory hubServicesFactory = new HubServicesFactory(HubServicesFactory.createDefaultGson(), HubServicesFactory.createDefaultJsonParser(), hubServerConfig.createApiTokenRestConnection(intLogger), intLogger);
                final ProjectService projectService = hubServicesFactory.createProjectService();
                final ProjectRequestBuilder projectRequestBuilder = new ProjectRequestBuilder();
                projectRequestBuilder.setProjectName("NexusTest");
                projectRequestBuilder.setVersionName(String.valueOf(version));
                projectRequestBuilder.setDistribution(ProjectVersionDistributionType.EXTERNAL);
                projectRequestBuilder.setPhase(ProjectVersionPhaseType.DEVELOPMENT);
                try {
                    projectService.createHubProject(projectRequestBuilder.build());
                    version++;
                } catch (final IntegrationException e) {
                    logger.error("Exception thrown while creating project: " + e.getMessage());
                }

                // TODO generate local file and scan it

            }
        }
    }

    // TODO Add filtering to allow only Artifacts through (Perhaps create custom filtering object)
    private boolean shouldScanAsset(final Asset asset) {
        return asset.blobRef().getBlobId() != null;
    }

    // This is used to Add items to the BlackDuck tab in the UI
    private NestedAttributesMap getBlackDuckNestedAttributes(final NestedAttributesMap nestedAttributesMap) {
        return nestedAttributesMap.child(BLACKDUCK_CATEGORY);
    }

    //    private ScanConfig getScanConfig() {
    //        final TaskConfiguration taskConfiguration = getConfiguration();
    //        final int scanMemory = taskConfiguration.getInteger(ScanTaskKeys.SCAN_MEMORY.getParameterKey(), ScanTaskFields.DEFAULT_SCAN_MEMORY);
    //        final boolean dryRun = false;
    //        final String installDirectory = taskConfiguration.getString(ScanTaskKeys.WORKING_DIRECTORY.getParameterKey(), ScanTaskFields.DEFAULT_WORKING_DIRECTORY);
    //        final String outputDirectory = "BlackDuck";
    //
    //        return new ScanConfig();
    //    }
    //
    //    private String getProjectName() {
    //        return "";
    //    }
    //
    //    private String getVersionName() {
    //
    //    }

    private HubServerConfig getHubServerConfig() {
        final HubCapabilityConfiguration hubCapabilityConfiguration = getCapabilityConfiguration();
        if (hubCapabilityConfiguration == null) {
            throw new TaskInterruptedException("Hub server config not set.", true);
        }
        return hubCapabilityConfiguration.createHubServerConfig();
    }

    private HubCapabilityConfiguration getCapabilityConfiguration() {
        final CapabilityReference capabilityReference = findCapabilityReference(HubCapability.class);
        if (capabilityReference == null) {
            logger.warn("Hub capability not created.");
            return null;
        }

        final HubCapability capability = capabilityReference.capabilityAs(HubCapability.class);
        logger.info("Getting HubCapability config");
        return capability.getConfig();
    }

    //TODO Find a better way to get the correct capability (Don't know what the ID is to use with get)
    private CapabilityReference findCapabilityReference(final Class<? extends CapabilitySupport<?>> capabilityClass) {
        final Collection<? extends CapabilityReference> capabilityReferenceList = capabilityRegistry.getAll();
        for (final CapabilityReference capabilityReference : capabilityReferenceList) {
            final String capabilityName = capabilityReference.capability().getClass().getName();
            if (capabilityName.equals(capabilityClass.getName())) {
                logger.debug("Found capability: " + capabilityName);
                return capabilityReference;
            }
        }
        return null;
    }

}
