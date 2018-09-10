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
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.capability.HubCapability;
import com.synopsys.integration.blackduck.nexus3.capability.HubCapabilityConfiguration;
import com.synopsys.integration.blackduck.nexus3.capability.HubCapabilityDescriptor;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;

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
        logAllCapabilities();
        final HubCapabilityConfiguration hubCapabilityConfiguration = getCapabilityConfiguration();
        if (hubCapabilityConfiguration == null) {
            throw new TaskInterruptedException("Hub server config not set.", true);
        }
        final HubServerConfig hubServerConfig = hubCapabilityConfiguration.createHubServerConfig();

        logger.info("Found repository: " + repository.getName());
        final Iterable<Asset> foundAssets = queryManager.findAssetsInRepository(repository);
        for (final Asset asset : foundAssets) {
            if (isAssetScannable(asset)) {
                logger.info("Scanning item: " + asset.name());

                // Scan item
                logger.info("HubServerConfig info");
                logger.info("Url:" + hubServerConfig.getHubUrl());
                logger.info("Username:" + hubServerConfig.getGlobalCredentials().getUsername());
                // FIXME Current issue with encryption/decryption (I believe related to the hub-common bug).
                //                try {
                //                    logger.info("Password:" + hubServerConfig.getGlobalCredentials().getDecryptedPassword());
                //                } catch (final EncryptionException e) {
                //                    e.printStackTrace();
                //                }
            }
        }
    }

    private void logAllCapabilities() {
        final Collection<? extends CapabilityReference> capabilityReferenceList = capabilityRegistry.getAll();
        for (final CapabilityReference capabilityReference : capabilityReferenceList) {
            final String capabilityName = capabilityReference.capability().getClass().getName();
            logger.info("Found capability: " + capabilityName);
        }
        logger.info("All Capabilities listed.");
    }

    // TODO Add filtering here to allow only Artifacts through
    private boolean isAssetScannable(final Asset asset) {
        return asset.componentId() != null;
    }

    private HubCapabilityConfiguration getCapabilityConfiguration() {
        final CapabilityReference capabilityReference = capabilityRegistry.get(CapabilityIdentity.capabilityIdentity(HubCapabilityDescriptor.CAPABILITY_ID));
        if (capabilityReference == null) {
            logger.warn("Hub capability not created.");
            return null;
        }

        final HubCapability capability = capabilityReference.capabilityAs(HubCapability.class);
        logger.info("Getting HubCapability config");
        return capability.getConfig();
    }

    private NestedAttributesMap getBlackDuckNestedAttributes(final NestedAttributesMap nestedAttributesMap) {
        return nestedAttributesMap.child(BLACKDUCK_CATEGORY);
    }

}
