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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonTaskFilters;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
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
    public static final String SCAN_CODE_LOCATION_NAME = "Nexus3Scan";
    private static final int SCAN_OUTPUT_LOCATION = 0;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final QueryManager queryManager;
    private final DateTimeParser dateTimeParser;
    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final ScanMetaDataProcessor scanMetaDataProcessor;
    private final CommonTaskFilters commonTaskFilters;

    @Inject
    public ScanTask(final QueryManager queryManager, final DateTimeParser dateTimeParser, final CommonRepositoryTaskHelper commonRepositoryTaskHelper, final ScanMetaDataProcessor scanMetaDataProcessor,
        final CommonTaskFilters commonTaskFilters) {
        this.queryManager = queryManager;
        this.dateTimeParser = dateTimeParser;
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.scanMetaDataProcessor = scanMetaDataProcessor;
        this.commonTaskFilters = commonTaskFilters;
    }

    @Override
    protected boolean appliesTo(final Repository repository) {
        return commonRepositoryTaskHelper.doesRepositoryApply(repository, getRepositoryField());
    }

    @Override
    public String getMessage() {
        return commonRepositoryTaskHelper.getTaskMessage(ScanTaskDescriptor.BLACK_DUCK_SCAN_TASK_NAME, getRepositoryField());
    }

    @Override
    protected void execute(final Repository repository) {
        final HubServerConfig hubServerConfig = commonRepositoryTaskHelper.getHubServerConfig();
        final ScanJobManager scanJobManager;
        final IntLogger intLogger = new Slf4jIntLogger(logger);
        try {
            scanJobManager = ScanJobManager.createDefaultScanManager(intLogger, hubServerConfig);
        } catch (final EncryptionException e) {
            intLogger.debug(e.getMessage(), e);
            throw new TaskInterruptedException("Problem creating ScanJobManager: " + e.getMessage(), true);
        }

        final File workingDirectory = commonRepositoryTaskHelper.getWorkingDirectory(taskConfiguration());
        final File workingBlackDuckDirectory = new File(workingDirectory, "blackduck");
        final File tempFileStorage = new File(workingBlackDuckDirectory, "temp");
        final File outputDirectory = new File(workingBlackDuckDirectory, "output");
        try {
            Files.createDirectories(tempFileStorage.toPath());
            Files.createDirectories(outputDirectory.toPath());
        } catch (final IOException e) {
            intLogger.debug(e.getMessage(), e);
            throw new TaskInterruptedException("Could not create directories to use with Scanner: " + e.getMessage(), true);
        }

        final boolean alwaysScan = taskConfiguration().getBoolean(ScanTaskDescriptor.KEY_ALWAYS_CHECK, false);
        final boolean redoFailures = taskConfiguration().getBoolean(ScanTaskDescriptor.KEY_REDO_FAILURES, false);

        final Query filteredQuery = commonRepositoryTaskHelper.createPagedQuery(Optional.empty()).build();
        PagedResult<Asset> foundAssets = commonRepositoryTaskHelper.retrievePagedAssets(repository, filteredQuery);
        while (foundAssets.hasResults()) {
            logger.debug("Found results from DB");
            final Map<String, AssetWrapper> scannedAssets = new HashMap<>();
            for (final Asset asset : foundAssets.getTypeList()) {
                final AssetWrapper assetWrapper = new AssetWrapper(asset, repository, queryManager);
                final String name = assetWrapper.getFullPath();
                final String version = assetWrapper.getVersion();
                final String repoName = repository.getName();
                final String codeLocationName = scanMetaDataProcessor.createCodeLocationName(repoName, name, version);

                final TaskStatus status = assetWrapper.getBlackDuckStatus();
                final boolean shouldScan = shouldScan(status, alwaysScan, redoFailures);
                logger.debug("Status matches, {}", shouldScan);
                final boolean shouldScanAgain = commonTaskFilters.hasAssetBeenModified(assetWrapper);
                logger.debug("Process again, {}", shouldScanAgain);
                final boolean scan = shouldScan || shouldScanAgain;
                logger.debug("Scan without filter check, {}", scan);
                if (commonTaskFilters.skipAssetProcessing(assetWrapper, taskConfiguration()) || !scan) {
                    logger.debug("Binary file did not meet requirements for scan: {}", name);
                    continue;
                }

                performScan(hubServerConfig, workingBlackDuckDirectory, outputDirectory, tempFileStorage, codeLocationName, assetWrapper, scanJobManager, scannedAssets);
                assetWrapper.updateAsset();
            }

            try {
                FileUtils.cleanDirectory(tempFileStorage);
                FileUtils.cleanDirectory(outputDirectory);
            } catch (final IOException e) {
                logger.warn("Problem cleaning scan directories {}", outputDirectory.getAbsolutePath());
            }

            final Query nextPageQuery = commonRepositoryTaskHelper.createPagedQuery(foundAssets.getLastName()).build();
            foundAssets = commonRepositoryTaskHelper.retrievePagedAssets(repository, nextPageQuery);

            for (final Map.Entry<String, AssetWrapper> entry : scannedAssets.entrySet()) {
                final AssetWrapper assetWrapper = entry.getValue();
                final String codeLocationName = entry.getKey();
                final String projectName = assetWrapper.getName();
                final String version = assetWrapper.getVersion();
                try {
                    final String uploadUrl = commonRepositoryTaskHelper.verifyUpload(codeLocationName, projectName, version);
                    scanMetaDataProcessor.updateRepositoryMetaData(assetWrapper, uploadUrl);
                } catch (final IntegrationException e) {
                    assetWrapper.removeAllBlackDuckData();
                    assetWrapper.addFailureToBlackDuckPanel(e.getMessage());
                    assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
                    assetWrapper.updateAsset();
                    logger.error("Problem communicating with BlackDuck: {}", e.getMessage());
                }
            }
        }
    }

    private boolean shouldScan(final TaskStatus status, final boolean alwaysScan, final boolean redoFailures) {
        if (TaskStatus.PENDING.equals(status) || TaskStatus.SUCCESS.equals(status)) {
            return alwaysScan;
        }

        if (TaskStatus.FAILURE.equals(status)) {
            return redoFailures;
        }

        return true;
    }

    private void performScan(final HubServerConfig hubServerConfig, final File workingBlackDuckDirectory, final File outputDirectory, final File tempFileStorage, final String codeLocationName, final AssetWrapper assetWrapper,
        final ScanJobManager scanJobManager, final Map<String, AssetWrapper> scannedAssets) {
        final String name = assetWrapper.getFullPath();
        final String projectName = assetWrapper.getName();
        final String version = assetWrapper.getVersion();

        logger.info("Scanning item: {}", name);
        final File binaryFile;
        try {
            binaryFile = assetWrapper.getBinaryBlobFile(tempFileStorage);
        } catch (final IOException e) {
            logger.debug("Exception thrown: {}", e.getMessage());
            throw new TaskInterruptedException("Error saving blob binary to file", true);
        }

        try {
            final ScanJob scanJob = createScanJob(hubServerConfig, workingBlackDuckDirectory, outputDirectory, projectName, version, binaryFile.getAbsolutePath(), codeLocationName);
            final ScanJobOutput scanJobOutput = scanJobManager.executeScans(scanJob);
            final List<ScanCommandOutput> scanOutputs = scanJobOutput.getScanCommandOutputs();
            final ScanCommandOutput scanCommandResult = scanOutputs.get(SCAN_OUTPUT_LOCATION);
            if (Result.SUCCESS == scanCommandResult.getResult()) {
                assetWrapper.addPendingToBlackDuckPanel("Component uploaded to BlackDuck, waiting for update.");
                scannedAssets.put(codeLocationName, assetWrapper);
            }
        } catch (final IOException | IntegrationException e) {
            assetWrapper.removeAllBlackDuckData();
            assetWrapper.addFailureToBlackDuckPanel(e.getMessage());
            logger.error("Error scanning asset: {}. Reason: {}", name, e.getMessage());
        } finally {
            assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
        }
    }

    private ScanJob createScanJob(final HubServerConfig hubServerConfig, final File workingBlackDuckDirectory, final File outputDirectory, final String projectName, final String projectVersion, final String pathToScan,
        final String codeLocationName)
        throws EncryptionException {
        final int scanMemory = taskConfiguration().getInteger(ScanTaskDescriptor.KEY_SCAN_MEMORY, ScanTaskDescriptor.DEFAULT_SCAN_MEMORY);

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
