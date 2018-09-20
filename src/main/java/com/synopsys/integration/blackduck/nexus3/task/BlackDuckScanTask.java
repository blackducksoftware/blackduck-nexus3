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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilitySupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.exception.HubIntegrationException;
import com.synopsys.integration.blackduck.nexus3.capability.HubCapability;
import com.synopsys.integration.blackduck.nexus3.capability.HubCapabilityConfiguration;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.scan.ScanConfig;
import com.synopsys.integration.blackduck.nexus3.scan.Scanner;
import com.synopsys.integration.blackduck.nexus3.task.model.ScanTaskFields;
import com.synopsys.integration.blackduck.nexus3.task.model.ScanTaskKeys;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.nexus3.ui.ComponentPanel;
import com.synopsys.integration.blackduck.nexus3.util.BlobFileCreator;
import com.synopsys.integration.blackduck.signaturescanner.ScanJob;
import com.synopsys.integration.blackduck.signaturescanner.ScanJobOutput;
import com.synopsys.integration.blackduck.signaturescanner.command.ScanCommandOutput;
import com.synopsys.integration.exception.EncryptionException;

@Named
public class BlackDuckScanTask extends RepositoryTaskSupport {
    private final Logger logger = createLogger();

    private final QueryManager queryManager;
    private final CapabilityRegistry capabilityRegistry;
    private final BlobFileCreator blobFileCreator;

    @Inject
    public BlackDuckScanTask(final QueryManager queryManager, final CapabilityRegistry capabilityRegistry, final BlobFileCreator blobFileCreator) {
        this.queryManager = queryManager;
        this.capabilityRegistry = capabilityRegistry;
        this.blobFileCreator = blobFileCreator;
    }

    @Override
    public String getMessage() {
        return "BlackDuck scanning repository " + getRepositoryField();
    }

    // TODO verify that the group repository will work accordingly here
    @Override
    protected boolean appliesTo(final Repository repository) {
        final String repositoryName = getRepositoryField();
        return repository.getName().equals(repositoryName);
    }

    @Override
    protected void execute(final Repository repository) {
        final HubServerConfig hubServerConfig = getHubServerConfig();
        final ScanConfig scanConfig = getScanConfig();
        final Scanner blackDuckScanner = new Scanner(hubServerConfig, scanConfig);
        logger.info("Found repository: " + repository.getName());
        final Iterable<Asset> foundAssets = queryManager.findAssetsInRepository(repository);
        for (final Asset asset : foundAssets) {
            if (shouldScan(asset)) {
                final Component component = queryManager.getComponent(repository, asset.componentId());
                final Blob binaryBlob = queryManager.getBlob(repository, asset.blobRef());
                logger.debug("Binary blob header contents: {}", binaryBlob.getHeaders().toString());
                final File workingDirectory = new File(scanConfig.getInstallDirectory(), "blackduck");
                if (workingDirectory.exists()) {
                    workingDirectory.mkdir();
                }
                try {
                    final File binaryFile = blobFileCreator.convertBlobToFile(binaryBlob, workingDirectory);
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                if (component != null && asset.blobRef().getBlobId() != null) {
                    logger.info("Scanning item: {}", component.name());
                    // TODO generate local file and scan it
                    final String name = component.name();
                    logger.debug("Using name {} for project", name);
                    final String version = component.version();
                    logger.debug("Using version {} for project", version);
                    try {
                        final ScanJob scanJob = blackDuckScanner.createScanJob("", name, version);
                        final ScanJobOutput scanJobOutput = blackDuckScanner.startScanJob(scanJob);
                        final List<ScanCommandOutput> scanOutputs = scanJobOutput.getScanCommandOutputs();
                        final ScanCommandOutput scanResult = scanOutputs.get(0);
                        final ComponentPanel componentPanel = new ComponentPanel(repository, component);
                        componentPanel.addToBlackDuckPanel(AssetPanelLabel.SCAN_STATUS, scanResult.getResult().name());
                        componentPanel.savePanel(queryManager);
                    } catch (final EncryptionException | IOException | HubIntegrationException e) {
                        logger.error("Error scanning asset: {}. Reason: {}", name, e.getMessage());
                    }
                }
            }

        }
    }

    // TODO add functionality to verify if a scan should occur
    private boolean shouldScan(final Asset asset) {
        return asset.componentId() != null;
    }

    private ScanConfig getScanConfig() {
        final TaskConfiguration taskConfiguration = getConfiguration();
        final int scanMemory = taskConfiguration.getInteger(ScanTaskKeys.SCAN_MEMORY.getParameterKey(), ScanTaskFields.DEFAULT_SCAN_MEMORY);
        final String installDirectory = taskConfiguration.getString(ScanTaskKeys.WORKING_DIRECTORY.getParameterKey(), ScanTaskFields.DEFAULT_WORKING_DIRECTORY);
        final String outputDirectory = "BlackDuck";

        return new ScanConfig(scanMemory, false, installDirectory, outputDirectory);
    }

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
