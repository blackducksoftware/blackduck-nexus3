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
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.CaseUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.api.generated.view.CodeLocationView;
import com.synopsys.integration.blackduck.api.view.ScanSummaryView;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
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
import com.synopsys.integration.blackduck.rest.BlackduckRestConnection;
import com.synopsys.integration.blackduck.service.HubService;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
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
        final String workingDirectory = getConfiguration().getString(ScanTaskKeys.WORKING_DIRECTORY.getParameterKey(), ScanTaskFields.DEFAULT_WORKING_DIRECTORY);
        final File workingBlackDuckDirectory = new File(workingDirectory, "blackduck");
        workingBlackDuckDirectory.mkdir();
        final File outputDirectory = new File(workingBlackDuckDirectory, "output");
        outputDirectory.mkdir();
        //        final Query.Builder filteredQuery = createFilteredQueryBuilder(scanTaskConfig);
        final Query.Builder filteredQuery = Query.builder();
        logger.debug("Using query to find artifacts: {}, with parameters: {}", filteredQuery.build().getWhere(), filteredQuery.build().getParameters());
        PagedResult<Asset> foundAssets = commonRepositoryTaskHelper.pagedAssets(repository, filteredQuery, Optional.empty(), 100);
        while (foundAssets.hasResults()) {
            for (final Asset asset : foundAssets.getTypeList()) {
                final AssetWrapper assetWrapper = new AssetWrapper(asset, repository, queryManager);
                final String filename = assetWrapper.getFilename();
                final boolean doesExtensionMatch = taskFilter.doesExtensionMatch(filename, scanTaskConfig.getFilePatterns());
                final boolean doesRepositoryPathMatch = taskFilter.doesRepositoryPathMatch(asset.name(), scanTaskConfig.getRepositoryPathRegex());

                if (!doesExtensionMatch || !doesRepositoryPathMatch) {
                    logger.debug("Binary file did not meet requirements for scan: {}", filename);
                    continue;
                }

                final String scanStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.TASK_STATUS);
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

                    final File binaryFile;
                    try {
                        binaryFile = assetWrapper.getBinaryBlobFile(workingBlackDuckDirectory);
                    } catch (final IOException e) {
                        try {
                            FileUtils.deleteDirectory(outputDirectory);
                        } catch (final IOException e1) {
                            logger.error("Error deleting output directory {}", outputDirectory.getAbsolutePath());
                        }
                        throw new TaskInterruptedException("Error saving blob binary to file", true);
                    }

                    try {
                        final ScanJob scanJob = createScanJob(hubServerConfig, workingBlackDuckDirectory, outputDirectory, name, version, binaryFile.getAbsolutePath());
                        final ScanJobOutput scanJobOutput = scanJobManager.executeScans(scanJob);
                        final List<ScanCommandOutput> scanOutputs = scanJobOutput.getScanCommandOutputs();
                        final ScanCommandOutput scanCommandResult = scanOutputs.get(SCAN_OUTPUT_LOCATION);
                        final String scanResult = scanCommandResult.getResult().name();
                        final Optional<File> scanCommandFileOutput = scanCommandResult.getScanSummaryFile();
                        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_STATUS, scanResult);
                        if (Result.SUCCESS.name().equals(scanResult) && scanCommandFileOutput.isPresent()) {
                            final String hubScanUrl = getHubResultUrl(scanCommandFileOutput.get(), hubServerConfig.createRestConnection(intLogger), intLogger);
                            assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.HUB_URL, hubScanUrl);
                            assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
                        }
                        queryManager.updateAsset(repository, asset);
                    } catch (final IOException | IntegrationException e) {
                        logger.error("Error scanning asset: {}. Reason: {}", name, e.getMessage());
                    }

                    if (binaryFile != null && binaryFile.exists()) {
                        FileUtils.deleteQuietly(binaryFile);
                    }
                }

            }

            foundAssets = commonRepositoryTaskHelper.pagedAssets(repository, filteredQuery, foundAssets.getLastName(), foundAssets.getLimit());
        }

        try {
            FileUtils.deleteDirectory(outputDirectory);
        } catch (final IOException e) {
            logger.error("Error deleting output directory {}", outputDirectory.getAbsolutePath());
        }
    }

    public String getHubResultUrl(final File resultsFile, final BlackduckRestConnection restConnection, final IntLogger intLogger) throws IOException, IntegrationException {
        final String resultsJson = FileUtils.readFileToString(resultsFile, Charset.defaultCharset());
        final HubServicesFactory hubServicesFactory = new HubServicesFactory(HubServicesFactory.createDefaultGson(), HubServicesFactory.createDefaultJsonParser(), restConnection, intLogger);
        final Gson gson = hubServicesFactory.getGson();
        final ScanSummaryView parsedResults = gson.fromJson(resultsJson, ScanSummaryView.class);
        final HubService hubService = hubServicesFactory.createHubService();
        final String codeLocationLink = hubService.getFirstLink(parsedResults, ScanSummaryView.CODELOCATION_LINK);
        final CodeLocationView codeLocationView = hubService.getResponse(codeLocationLink, CodeLocationView.class);
        return codeLocationView.mappedProjectVersion;
    }

    private Query.Builder createFilteredQueryBuilder(final ScanTaskConfig scanTaskConfig) {
        final Query.Builder queryBuilder = Query.builder();

        final DateTime artifactCutoffDate = scanTaskConfig.getOldArtifactCutoffDate();
        queryBuilder.where("component.blobUpdated >= ").param(artifactCutoffDate);

        final String repositoryPathRegex = scanTaskConfig.getRepositoryPathRegex();
        queryBuilder.and("component.name REGEXP ").param(repositoryPathRegex);

        final String statusWhere = getDbXmlPath(AssetPanelLabel.TASK_STATUS) + " != ";
        if (!scanTaskConfig.isRescanFailures()) {
            queryBuilder.and(statusWhere).param(Result.FAILURE.name());
        }
        if (!scanTaskConfig.isAlwaysScan()) {
            queryBuilder.and(statusWhere).param(Result.SUCCESS.name());
        }

        return queryBuilder;
    }

    private String getDbXmlPath(final AssetPanelLabel assetPanelLabel) {
        final String dbXmlPath = "component.attributes.blackDuck.";
        return dbXmlPath + CaseUtils.toCamelCase(assetPanelLabel.getLabel(), false);
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

    private ScanJob createScanJob(final HubServerConfig hubServerConfig, final File workingBlackDuckDirectory, final File outputDirectory, final String projectName, final String projectVersion, final String pathToScan)
        throws EncryptionException {
        final TaskConfiguration taskConfiguration = getConfiguration();
        final int scanMemory = taskConfiguration.getInteger(ScanTaskKeys.SCAN_MEMORY.getParameterKey(), ScanTaskFields.DEFAULT_SCAN_MEMORY);

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
