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
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.exception.HubIntegrationException;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.TaskFilter;
import com.synopsys.integration.blackduck.nexus3.task.scan.model.ScanTaskConfig;
import com.synopsys.integration.blackduck.nexus3.task.scan.model.ScanTaskFields;
import com.synopsys.integration.blackduck.nexus3.task.scan.model.ScanTaskKeys;
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
import com.synopsys.integration.log.Slf4jIntLogger;

@Named
public class ScanTask extends RepositoryTaskSupport {
    private static final int SCAN_OUTPUT_LOCATION = 0;
    private static int paramCounter;
    private final Logger logger = createLogger();

    private final QueryManager queryManager;
    private final DateTimeParser dateTimeParser;
    private final TaskFilter taskFilter;
    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;

    @Inject
    public ScanTask(final QueryManager queryManager, final DateTimeParser dateTimeParser, final TaskFilter taskFilter, final CommonRepositoryTaskHelper commonRepositoryTaskHelper) {
        this.queryManager = queryManager;
        this.dateTimeParser = dateTimeParser;
        this.taskFilter = taskFilter;
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
    }

    @Override
    protected boolean appliesTo(final Repository repository) {
        return commonRepositoryTaskHelper.doesRepositoryApply(repository, getRepositoryField());
    }

    @Override
    public String getMessage() {
        return commonRepositoryTaskHelper.getTaskMessage("Hub Scan", getRepositoryField());
    }

    @Override
    protected void execute(final Repository repository) {
        final HubServerConfig hubServerConfig = commonRepositoryTaskHelper.getHubServerConfig();
        final ScanTaskConfig scanTaskConfig = getTaskConfig();
        final ScanJobManager scanJobManager;
        try {
            scanJobManager = ScanJobManager.createDefaultScanManager(new Slf4jIntLogger(logger), hubServerConfig);
        } catch (final EncryptionException e) {
            throw new TaskInterruptedException("Problem creating ScanJobManager: " + e.getMessage(), true);

        }
        final String workingDirectory = getConfiguration().getString(ScanTaskKeys.WORKING_DIRECTORY.getParameterKey(), ScanTaskFields.DEFAULT_WORKING_DIRECTORY);
        final File workingBlackDuckDirectory = new File(workingDirectory, "blackduck");
        workingBlackDuckDirectory.mkdir();
        final PagedResult<Asset> foundAssets = commonRepositoryTaskHelper.pagedAssets(repository, Query.builder(), Optional.empty(), 100);
        for (final Asset asset : foundAssets.getTypeList()) {
            final AssetWrapper assetWrapper = new AssetWrapper(asset, repository, queryManager);
            final String filename = assetWrapper.getFilename();

            final boolean doesExtensionMatch = taskFilter.doesExtensionMatch(filename, scanTaskConfig.getFilePatterns());
            final boolean doesRepositoryPathMatch = taskFilter.doesRepositoryPathMatch(asset.name(), scanTaskConfig.getRepositoryPathRegex());

            if (!doesExtensionMatch || !doesRepositoryPathMatch) {
                logger.debug("Binary file did not meet requirements for scan: {}", filename);
                continue;
            }

            final File binaryFile;
            try {
                binaryFile = assetWrapper.getBinaryBlobFile(workingBlackDuckDirectory);
            } catch (final IOException e) {
                throw new TaskInterruptedException("Error saving blob binary to file", true);
            }

            final String scanStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.SCAN_STATUS);

            boolean shouldScan = scanStatus == null;
            if (!shouldScan) {
                shouldScan = !taskFilter.didArtifactFailScan(Enum.valueOf(Result.class, scanStatus)) || scanTaskConfig.isRescanFailures();
            }

            final boolean artifactTooOld = taskFilter.isArtifactTooOld(scanTaskConfig.getOldArtifactCutoffDate(), assetWrapper.getComponentLastUpdated());
            final boolean alwaysScan = !artifactTooOld && scanTaskConfig.isAlwaysScan();

            final String name = assetWrapper.getName();
            final String version = assetWrapper.getVersion();
            if (alwaysScan || (!artifactTooOld && shouldScan)) {
                logger.info("Scanning item: {}", name);
                try {
                    final ScanJob scanJob = createScanJob(hubServerConfig, workingBlackDuckDirectory, name, version, binaryFile.getAbsolutePath());
                    final ScanJobOutput scanJobOutput = scanJobManager.executeScans(scanJob);
                    final List<ScanCommandOutput> scanOutputs = scanJobOutput.getScanCommandOutputs();
                    final ScanCommandOutput scanResult = scanOutputs.get(SCAN_OUTPUT_LOCATION);
                    assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.SCAN_STATUS, scanResult.getResult().name());
                    queryManager.updateAsset(repository, asset);
                } catch (final EncryptionException | IOException | HubIntegrationException e) {
                    logger.error("Error scanning asset: {}. Reason: {}", name, e.getMessage());
                }
            }
        }
    }

    private Query.Builder createFilteredQueryBuilder(final ScanTaskConfig scanTaskConfig) {
        final Query.Builder queryBuilder = Query.builder();

        final DateTime artifactCutoffDate = scanTaskConfig.getOldArtifactCutoffDate();
        final String repositoryPathRegex = scanTaskConfig.getRepositoryPathRegex();

        queryBuilder.where("name REGEXP ").param(repositoryPathRegex);

        if (scanTaskConfig.isRescanFailures()) {
            queryBuilder.and("component.attributes.blackDuck.scanStatus = ").param(Result.FAILURE.name());
        }
        if (scanTaskConfig.isAlwaysScan()) {
            queryBuilder.and("component.attributes.blackDuck.scanStatus = ").param(Result.SUCCESS.name());
        }

        return queryBuilder;
    }

    private ScanTaskConfig getTaskConfig() {
        final String filePatterns = getConfiguration().getString(ScanTaskKeys.FILE_PATTERNS.getParameterKey());
        final String artifactPath = getConfiguration().getString(ScanTaskKeys.REPOSITORY_PATH.getParameterKey());
        final boolean rescanFailures = getConfiguration().getBoolean(ScanTaskKeys.RESCAN_FAILURES.getParameterKey(), false);
        final boolean alwaysScan = getConfiguration().getBoolean(ScanTaskKeys.ALWAYS_SCAN.getParameterKey(), false);

        final String artifactCutoff = getConfiguration().getString(ScanTaskKeys.OLD_ARTIFACT_CUTOFF.getParameterKey());
        final DateTime oldArtifactCutoffDate = dateTimeParser.convertFromStringToDate(artifactCutoff);
        return new ScanTaskConfig(filePatterns, artifactPath, oldArtifactCutoffDate, rescanFailures, alwaysScan);
    }

    private ScanJob createScanJob(final HubServerConfig hubServerConfig, final File workingBlackDuckDirectory, final String projectName, final String projectVersion, final String pathToScan) throws EncryptionException {
        final TaskConfiguration taskConfiguration = getConfiguration();
        final int scanMemory = taskConfiguration.getInteger(ScanTaskKeys.SCAN_MEMORY.getParameterKey(), ScanTaskFields.DEFAULT_SCAN_MEMORY);
        final File outputDirectory = new File(workingBlackDuckDirectory, "output");
        outputDirectory.mkdir();

        final ScanJobBuilder scanJobBuilder = new ScanJobBuilder()
                                                  .fromHubServerConfig(hubServerConfig)
                                                  .scanMemoryInMegabytes(scanMemory)
                                                  .installDirectory(workingBlackDuckDirectory)
                                                  .outputDirectory(outputDirectory)
                                                  .projectAndVersionNames(projectName, projectVersion);
        final ScanTarget scanTarget = ScanTarget.createBasicTarget(pathToScan);
        scanJobBuilder.addTarget(scanTarget);

        final ScanJob scanJob = scanJobBuilder.build();
        return scanJob;
    }
}
