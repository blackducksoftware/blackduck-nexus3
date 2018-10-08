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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.TaskFilter;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
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
        final IntLogger intLogger = new Slf4jIntLogger(logger);
        try {
            scanJobManager = ScanJobManager.createDefaultScanManager(intLogger, hubServerConfig);
        } catch (final EncryptionException e) {
            throw new TaskInterruptedException("Problem creating ScanJobManager: " + e.getMessage(), true);
        }

        final String workingDirectory = getConfiguration().getString(ScanTaskKeys.WORKING_DIRECTORY.getParameterKey(), ScanTaskDescriptor.DEFAULT_WORKING_DIRECTORY);
        final File workingBlackDuckDirectory = new File(workingDirectory, "blackduck");
        workingBlackDuckDirectory.mkdir();
        final File outputDirectory = new File(workingBlackDuckDirectory, "output");
        outputDirectory.mkdir();

        final Set<AssetWrapper> scannedAssets = new HashSet<>();
        final Query.Builder filteredQuery = createFilteredQueryBuilder(scanTaskConfig, Optional.empty(), scanTaskConfig.getLimit());
        PagedResult<Asset> foundAssets = commonRepositoryTaskHelper.pagedAssets(repository, filteredQuery.build());
        while (foundAssets.hasResults()) {
            logger.debug("Found results from DB");
            for (final Asset asset : foundAssets.getTypeList()) {
                final AssetWrapper assetWrapper = new AssetWrapper(asset, repository, queryManager);
                final String filename = assetWrapper.getFilename();
                final DateTime lastModified = assetWrapper.getComponentLastUpdated();
                final boolean doesExtensionMatch = taskFilter.doesExtensionMatch(filename, scanTaskConfig.getExtensionPatterns());
                final boolean doesRepositoryPathMatch = taskFilter.doesRepositoryPathMatch(asset.name(), scanTaskConfig.getRepositoryPathRegex());
                final boolean isArtifactTooOld = taskFilter.isArtifactTooOld(scanTaskConfig.getOldArtifactCutoffDate(), lastModified);

                if (!doesExtensionMatch || !doesRepositoryPathMatch || isArtifactTooOld) {
                    logger.debug("Binary file did not meet requirements for scan: {}", filename);
                    continue;
                }
                final String name = assetWrapper.getName();
                final String version = assetWrapper.getVersion();
                logger.info("Scanning item: {}", name);
                final File binaryFile;
                try {
                    binaryFile = assetWrapper.getBinaryBlobFile(workingBlackDuckDirectory);
                } catch (final IOException e) {
                    try {
                        FileUtils.deleteDirectory(outputDirectory);
                    } catch (final IOException e1) {
                        logger.error("Error deleting output directory {}", outputDirectory.getAbsolutePath());
                    }
                    logger.debug("Exception thrown: {}", e.getMessage());
                    throw new TaskInterruptedException("Error saving blob binary to file", true);
                }

                try {
                    final ScanJob scanJob = createScanJob(hubServerConfig, workingBlackDuckDirectory, outputDirectory, name, version, binaryFile.getAbsolutePath());
                    final ScanJobOutput scanJobOutput = scanJobManager.executeScans(scanJob);
                    final List<ScanCommandOutput> scanOutputs = scanJobOutput.getScanCommandOutputs();
                    final ScanCommandOutput scanCommandResult = scanOutputs.get(SCAN_OUTPUT_LOCATION);
                    TaskStatus taskStatus = TaskStatus.PENDING;
                    if (Result.SUCCESS != scanCommandResult.getResult()) {
                        taskStatus = TaskStatus.FAILURE;
                    }
                    assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_STATUS, taskStatus.name());
                    assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getNexusFormattedDate(new DateTime()));
                    logger.debug("Updating asset panel");
                    assetWrapper.updateAsset();
                    scannedAssets.add(assetWrapper);
                } catch (final IOException | IntegrationException e) {
                    logger.error("Error scanning asset: {}. Reason: {}", name, e.getMessage());
                }

                if (binaryFile != null && binaryFile.exists()) {
                    FileUtils.deleteQuietly(binaryFile);
                }

            }

            final Query.Builder nextPageQuery = createFilteredQueryBuilder(scanTaskConfig, foundAssets.getLastName(), scanTaskConfig.getLimit());
            foundAssets = commonRepositoryTaskHelper.pagedAssets(repository, nextPageQuery.build());
        }

        try {
            commonRepositoryTaskHelper.verifyAndMarkUpload(scannedAssets, hubServerConfig);
        } catch (final IntegrationException e) {
            logger.debug("Issue with BlackDuck: {}", e.getMessage());
            throw new TaskInterruptedException("There was a problem communicating with BlackDuck", true);
        } catch (final InterruptedException e) {
            logger.error("There was an issue when checking scan status: {}", e.getMessage());
        } finally {
            try {
                FileUtils.deleteDirectory(outputDirectory);
            } catch (final IOException e) {
                logger.warn("Problem deleting output directory {}", outputDirectory.getAbsolutePath());
            }
        }
    }

    private Query.Builder createFilteredQueryBuilder(final ScanTaskConfig scanTaskConfig, final Optional<String> lastNameUsed, final int limit) {
        final Query.Builder baseQueryBuilder = commonRepositoryTaskHelper.createPagedQuery(lastNameUsed, limit);

        //        final DateTime artifactCutoffDate = scanTaskConfig.getOldArtifactCutoffDate();
        //        if (artifactCutoffDate != null) {
        //            final String lastModifiedPath = "attributes.content.last_modified";
        //            baseQueryBuilder.and(lastModifiedPath + " > " + dateTimeParser.convertFromDateTimeToMillis(artifactCutoffDate));
        //        }
        //
        //        final String repositoryPathRegex = scanTaskConfig.getRepositoryPathRegex();
        //        if (StringUtils.isNotBlank(repositoryPathRegex)) {
        //            baseQueryBuilder.and("name MATCHES ").param(repositoryPathRegex);
        //        }

        final String statusPath = commonRepositoryTaskHelper.getBlackduckPanelPath(AssetPanelLabel.TASK_STATUS);

        if (!scanTaskConfig.isRescanFailures()) {
            baseQueryBuilder.and(statusPath + " != ").param(TaskStatus.FAILURE.name());
        }
        if (!scanTaskConfig.isAlwaysScan()) {
            baseQueryBuilder.and(statusPath + " != ").param(TaskStatus.SUCCESS.name());
            baseQueryBuilder.and(statusPath + " != ").param(TaskStatus.PENDING.name());
        }

        //        final String artifactExtensions = scanTaskConfig.getExtensionPatterns();
        //        final List<String> extensions = Arrays.stream(artifactExtensions.split(",")).map(String::trim).collect(Collectors.toList());
        //        final String extensionsWhereStatement = createExtensionsWhereStatement(extensions);
        //        baseQueryBuilder.and("name").like(extensions.get(0));
        //        baseQueryBuilder.and(extensionsWhereStatement);

        return baseQueryBuilder;
    }

    //    private String createExtensionsWhereStatement(final List<String> extensions) {
    //        final StringBuilder extensionsWhereBuilder = new StringBuilder();
    //        extensionsWhereBuilder.append("(");
    //        for (int extCount = 0; extCount < extensions.size(); extCount++) {
    //            final String extension = extensions.get(extCount);
    //            if (extCount == 0) {
    //                extensionsWhereBuilder.append("component LIKE %");
    //                extensionsWhereBuilder.append(extension);
    //            } else {
    //                extensionsWhereBuilder.append(" OR component LIKE %");
    //                extensionsWhereBuilder.append(extension);
    //            }
    //        }
    //        extensionsWhereBuilder.append(")");
    //        return extensionsWhereBuilder.toString();
    //    }

    private ScanTaskConfig getTaskConfig() {
        final String filePatterns = getConfiguration().getString(ScanTaskKeys.FILE_PATTERNS.getParameterKey());
        final String artifactPath = getConfiguration().getString(ScanTaskKeys.REPOSITORY_PATH.getParameterKey());
        final boolean rescanFailures = getConfiguration().getBoolean(ScanTaskKeys.RESCAN_FAILURES.getParameterKey(), false);
        final boolean alwaysScan = getConfiguration().getBoolean(ScanTaskKeys.ALWAYS_SCAN.getParameterKey(), false);
        final int limit = getConfiguration().getInteger(ScanTaskKeys.PAGING_SIZE.getParameterKey(), ScanTaskDescriptor.DEFAULT_SCAN_PAGE_SIZE);

        final String artifactCutoff = getConfiguration().getString(ScanTaskKeys.OLD_ARTIFACT_CUTOFF.getParameterKey());
        final DateTime oldArtifactCutoffDate = dateTimeParser.convertFromStringToDate(artifactCutoff);
        return new ScanTaskConfig(filePatterns, artifactPath, oldArtifactCutoffDate, rescanFailures, alwaysScan, limit);
    }

    private ScanJob createScanJob(final HubServerConfig hubServerConfig, final File workingBlackDuckDirectory, final File outputDirectory, final String projectName, final String projectVersion, final String pathToScan)
        throws EncryptionException {
        final TaskConfiguration taskConfiguration = getConfiguration();
        final int scanMemory = taskConfiguration.getInteger(ScanTaskKeys.SCAN_MEMORY.getParameterKey(), ScanTaskDescriptor.DEFAULT_SCAN_MEMORY);

        final ScanJobBuilder scanJobBuilder = new ScanJobBuilder()
                                                  .fromHubServerConfig(hubServerConfig)
                                                  .scanMemoryInMegabytes(scanMemory)
                                                  .installDirectory(workingBlackDuckDirectory)
                                                  .outputDirectory(outputDirectory)
                                                  .projectAndVersionNames(projectName, projectVersion);
        final ScanTarget scanTarget = ScanTarget.createBasicTarget(pathToScan);
        scanJobBuilder.addTarget(scanTarget);

        return scanJobBuilder.build();
    }
}
