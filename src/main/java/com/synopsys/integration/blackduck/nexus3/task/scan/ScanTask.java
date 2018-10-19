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
package com.synopsys.integration.blackduck.nexus3.task.scan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.api.generated.component.RiskCountView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomPolicyStatusView;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.CommonTaskConfig;
import com.synopsys.integration.blackduck.nexus3.task.CommonTaskKeys;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.task.metadata.MetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.task.metadata.VulnerabilityLevels;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.nexus3.util.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.util.DateTimeParser;
import com.synopsys.integration.blackduck.signaturescanner.ScanJob;
import com.synopsys.integration.blackduck.signaturescanner.ScanJobBuilder;
import com.synopsys.integration.blackduck.signaturescanner.ScanJobManager;
import com.synopsys.integration.blackduck.signaturescanner.ScanJobOutput;
import com.synopsys.integration.blackduck.signaturescanner.command.ScanCommandOutput;
import com.synopsys.integration.blackduck.signaturescanner.command.ScanTarget;
import com.synopsys.integration.blackduck.summary.Result;
import com.synopsys.integration.exception.EncryptionException;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

@Named
public class ScanTask extends RepositoryTaskSupport {
    private static final int SCAN_OUTPUT_LOCATION = 0;
    private final Logger logger = createLogger();

    private final QueryManager queryManager;
    private final DateTimeParser dateTimeParser;
    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final MetaDataProcessor metaDataProcessor;

    @Inject
    public ScanTask(final QueryManager queryManager, final DateTimeParser dateTimeParser, final CommonRepositoryTaskHelper commonRepositoryTaskHelper, final MetaDataProcessor metaDataProcessor) {
        this.queryManager = queryManager;
        this.dateTimeParser = dateTimeParser;
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.metaDataProcessor = metaDataProcessor;
    }

    @Override
    protected boolean appliesTo(final Repository repository) {
        return commonRepositoryTaskHelper.doesRepositoryApply(repository, getRepositoryField());
    }

    @Override
    public String getMessage() {
        return commonRepositoryTaskHelper.getTaskMessage("BlackDuck Scan", getRepositoryField());
    }

    @Override
    protected void execute(final Repository repository) {
        final HubServerConfig hubServerConfig = commonRepositoryTaskHelper.getHubServerConfig();
        final CommonTaskConfig commonTaskConfig = commonRepositoryTaskHelper.getTaskConfig(getConfiguration());
        final ScanJobManager scanJobManager;
        final IntLogger intLogger = new Slf4jIntLogger(logger);
        try {
            scanJobManager = ScanJobManager.createDefaultScanManager(intLogger, hubServerConfig);
        } catch (final EncryptionException e) {
            throw new TaskInterruptedException("Problem creating ScanJobManager: " + e.getMessage(), true);
        }

        final File workingBlackDuckDirectory = new File(commonTaskConfig.getWorkingDirectory(), "blackduck");
        final File tempFileStorage = new File(workingBlackDuckDirectory, "temp");
        final File outputDirectory = new File(workingBlackDuckDirectory, "output");
        try {
            Files.createDirectories(tempFileStorage.toPath());
            Files.createDirectories(outputDirectory.toPath());
        } catch (final IOException e) {
            e.printStackTrace();
            throw new TaskInterruptedException("Could not create directories to use with Scanner: " + e.getMessage(), true);
        }

        final Query filteredQuery = commonRepositoryTaskHelper.createFilteredQueryBuilder(commonTaskConfig, Optional.empty());
        PagedResult<Asset> foundAssets = commonRepositoryTaskHelper.pagedAssets(repository, filteredQuery);
        while (foundAssets.hasResults()) {
            logger.debug("Found results from DB");
            final Map<String, AssetWrapper> assetWrappers = new HashMap<>();
            for (final Asset asset : foundAssets.getTypeList()) {
                final AssetWrapper assetWrapper = new AssetWrapper(asset, repository, queryManager);
                final String name = assetWrapper.getName();
                final String version = assetWrapper.getVersion();
                final String codeLocationName = String.join("/", name, version, "Nexus3Scan");

                if (commonRepositoryTaskHelper.skipAssetProcessing(assetWrapper, commonTaskConfig)) {
                    logger.debug("Binary file did not meet requirements for scan: {}", name);
                    continue;
                }

                performScan(hubServerConfig, workingBlackDuckDirectory, outputDirectory, tempFileStorage, codeLocationName, assetWrapper, scanJobManager, assetWrappers);
            }

            try {
                FileUtils.cleanDirectory(tempFileStorage);
                FileUtils.cleanDirectory(outputDirectory);
            } catch (final IOException e) {
                logger.warn("Problem cleaning scan directories {}", outputDirectory.getAbsolutePath());
            }

            final Query nextPageQuery = commonRepositoryTaskHelper.createFilteredQueryBuilder(commonTaskConfig, foundAssets.getLastName());
            foundAssets = commonRepositoryTaskHelper.pagedAssets(repository, nextPageQuery);

            updatePanel(assetWrappers);
        }

    }

    private void performScan(final HubServerConfig hubServerConfig, final File workingBlackDuckDirectory, final File outputDirectory, final File tempFileStorage, final String codeLocationName, final AssetWrapper assetWrapper,
        final ScanJobManager scanJobManager, final Map<String, AssetWrapper> scannedAssets) {
        final String name = assetWrapper.getName();
        final String version = assetWrapper.getVersion();

        logger.info("Scanning item: {}", name);
        final File binaryFile;
        try {
            binaryFile = assetWrapper.getBinaryBlobFile(tempFileStorage);
        } catch (final IOException e) {
            logger.debug("Exception thrown: {}", e.getMessage());
            throw new TaskInterruptedException("Error saving blob binary to file", true);
        }

        TaskStatus taskStatus = TaskStatus.FAILURE;
        try {
            final ScanJob scanJob = createScanJob(hubServerConfig, workingBlackDuckDirectory, outputDirectory, name, version, binaryFile.getAbsolutePath(), codeLocationName);
            final ScanJobOutput scanJobOutput = scanJobManager.executeScans(scanJob);
            final List<ScanCommandOutput> scanOutputs = scanJobOutput.getScanCommandOutputs();
            final ScanCommandOutput scanCommandResult = scanOutputs.get(SCAN_OUTPUT_LOCATION);
            if (Result.SUCCESS == scanCommandResult.getResult()) {
                taskStatus = TaskStatus.PENDING;
                scannedAssets.put(codeLocationName, assetWrapper);
            }
        } catch (final IOException | IntegrationException e) {
            logger.error("Error scanning asset: {}. Reason: {}", name, e.getMessage());
        } finally {
            assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_STATUS, taskStatus.name());
            assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
            logger.debug("Updating asset panel");
            assetWrapper.updateAsset();
        }
    }

    private void updatePanel(final Map<String, AssetWrapper> assetWrappers) {
        for (final AssetWrapper assetWrapper : assetWrappers.values()) {
            final String name = assetWrapper.getName();
            final String version = assetWrapper.getVersion();

            final String uploadUrl = commonRepositoryTaskHelper.verifyUpload(new ArrayList<>(assetWrappers.keySet()), name, version);
            TaskStatus status = TaskStatus.SUCCESS;

            try {
                logger.info("Checking vulnerabilities.");
                final List<VersionBomComponentView> versionBomComponentViews = metaDataProcessor.checkAssetVulnerabilities(name, version);
                final VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();
                for (final VersionBomComponentView versionBomComponentView : versionBomComponentViews) {
                    logger.debug("Adding vulnerable component {}, version {}", versionBomComponentView.componentName, versionBomComponentView.componentVersion);
                    final List<RiskCountView> vulnerabilities = versionBomComponentView.securityRiskProfile.counts;
                    metaDataProcessor.addMaxAssetVulnerabilityCounts(vulnerabilities, vulnerabilityLevels);
                }
                logger.debug("Updating asset with Vulnerability info.");
                metaDataProcessor.updateAssetVulnerabilityData(vulnerabilityLevels, assetWrapper);
                logger.info("Checking policies.");
                final VersionBomPolicyStatusView policyStatusView = metaDataProcessor.checkAssetPolicy(name, version);
                logger.debug("Updating asset with Policy info.");
                metaDataProcessor.updateAssetPolicyData(policyStatusView, assetWrapper);
            } catch (final IntegrationException e) {
                status = TaskStatus.FAILURE;
                metaDataProcessor.removePolicyData(assetWrapper);
                metaDataProcessor.removeAssetVulnerabilityData(assetWrapper);
                logger.error("Problem retrieving status properties from BlackDuck: {}", e.getMessage());
            }

            commonRepositoryTaskHelper.addFinalPanelElements(assetWrapper, uploadUrl, status.name());
        }
    }

    private ScanJob createScanJob(final HubServerConfig hubServerConfig, final File workingBlackDuckDirectory, final File outputDirectory, final String projectName, final String projectVersion, final String pathToScan,
        final String codeLocationName)
        throws EncryptionException {
        final TaskConfiguration taskConfiguration = getConfiguration();
        final int scanMemory = taskConfiguration.getInteger(CommonTaskKeys.MAX_MEMORY.getParameterKey(), ScanTaskDescriptor.DEFAULT_SCAN_MEMORY);

        final ScanJobBuilder scanJobBuilder = new ScanJobBuilder()
                                                  .fromHubServerConfig(hubServerConfig)
                                                  .scanMemoryInMegabytes(scanMemory)
                                                  .installDirectory(workingBlackDuckDirectory)
                                                  .outputDirectory(outputDirectory)
                                                  .projectAndVersionNames(projectName, projectVersion);
        final ScanTarget scanTarget = ScanTarget.createBasicTarget(pathToScan, null, codeLocationName);
        scanJobBuilder.addTarget(scanTarget);

        return scanJobBuilder.build();
    }
}
