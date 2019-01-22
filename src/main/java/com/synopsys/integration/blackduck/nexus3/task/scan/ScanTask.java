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
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationData;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatch;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatchBuilder;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatchOutput;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatchRunner;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.SignatureScannerService;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.command.ScanTarget;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonTaskFilters;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.phonehome.PhoneHomeResponse;

@Named
public class ScanTask extends RepositoryTaskSupport {
    public static final String SCAN_CODE_LOCATION_NAME = "Nexus3Scan";
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
        final IntLogger intLogger = new Slf4jIntLogger(logger);

        String exceptionMessage = null;
        BlackDuckServerConfig blackDuckServerConfig = null;
        SignatureScannerService signatureScannerService = null;
        ProjectService projectService = null;
        Optional<PhoneHomeResponse> phoneHomeResponse = Optional.empty();
        try {
            blackDuckServerConfig = commonRepositoryTaskHelper.getBlackDuckServerConfig();
            final BlackDuckServicesFactory blackDuckServicesFactory = commonRepositoryTaskHelper.getBlackDuckServicesFactory();
            signatureScannerService = blackDuckServicesFactory.createSignatureScannerService(ScanBatchRunner.createDefault(intLogger, blackDuckServerConfig));
            projectService = blackDuckServicesFactory.createProjectService();
            phoneHomeResponse = commonRepositoryTaskHelper.phoneHome(ScanTaskDescriptor.BLACK_DUCK_SCAN_TASK_ID);
        } catch (final IntegrationException | IllegalStateException e) {
            logger.error("Black Duck hub server config invalid. " + e.getMessage(), e);
            exceptionMessage = e.getMessage();
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
        for (final Repository foundRepository : commonTaskFilters.findRelevantRepositories(repository)) {
            if (commonTaskFilters.isHostedRepository(foundRepository.getType())) {
                final String repoName = foundRepository.getName();
                logger.info("Checking repository for assets: {}", repoName);
                final Query filteredQuery = commonRepositoryTaskHelper.createPagedQuery(Optional.empty()).build();
                PagedResult<Asset> foundAssets = commonRepositoryTaskHelper.retrievePagedAssets(foundRepository, filteredQuery);

                while (foundAssets.hasResults()) {
                    logger.debug("Found results from DB");
                    final Map<CodeLocationCreationData<ScanBatchOutput>, AssetWrapper> scannedAssets = new HashMap<>();
                    for (final Asset asset : foundAssets.getTypeList()) {
                        final AssetWrapper assetWrapper = AssetWrapper.createScanAssetWrapper(asset, foundRepository, queryManager);
                        final String name = assetWrapper.getFullPath();
                        final String version = assetWrapper.getVersion();
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

                        if (StringUtils.isNotBlank(exceptionMessage)) {
                            commonRepositoryTaskHelper.failedConnection(assetWrapper, exceptionMessage);
                            assetWrapper.updateAsset();
                        } else if (null != blackDuckServerConfig && null != signatureScannerService) {
                            final CodeLocationCreationData<ScanBatchOutput> scanData = performScan(blackDuckServerConfig, workingBlackDuckDirectory, outputDirectory, tempFileStorage, codeLocationName, assetWrapper, signatureScannerService);
                            scannedAssets.put(scanData, assetWrapper);
                            assetWrapper.updateAsset();
                        }
                    }

                    try {
                        FileUtils.cleanDirectory(tempFileStorage);
                        FileUtils.cleanDirectory(outputDirectory);
                    } catch (final IOException e) {
                        logger.warn("Problem cleaning scan directories {}", outputDirectory.getAbsolutePath());
                    }

                    final Query nextPageQuery = commonRepositoryTaskHelper.createPagedQuery(foundAssets.getLastName()).build();
                    foundAssets = commonRepositoryTaskHelper.retrievePagedAssets(foundRepository, nextPageQuery);

                    if (null != signatureScannerService && null != projectService) {
                        for (final Map.Entry<CodeLocationCreationData<ScanBatchOutput>, AssetWrapper> entry : scannedAssets.entrySet()) {
                            final AssetWrapper assetWrapper = entry.getValue();
                            final CodeLocationCreationData<ScanBatchOutput> scanData = entry.getKey();
                            final String projectName = assetWrapper.getName();
                            final String version = assetWrapper.getVersion();
                            try {
                                if (!scanData.getOutput().getSuccessfulCodeLocationNames().isEmpty()) {
                                    signatureScannerService.waitForSignatureScan(scanData.getNotificationTaskRange(), scanData.getOutput().getSuccessfulCodeLocationNames(), blackDuckServerConfig.getTimeout() * 5);

                                    final ProjectVersionView projectVersionView = scanMetaDataProcessor.getOrCreateProjectVersion(projectService, projectName, version);
                                    if (null != projectVersionView) {
                                        scanMetaDataProcessor
                                            .updateRepositoryMetaData(projectService, assetWrapper, projectVersionView.getHref().orElse(blackDuckServerConfig.getBlackDuckUrl().toString()), projectVersionView);
                                    } else {
                                        updateAssetWrapperWithError(assetWrapper, String.format("Could not find project %s and version %s after the scan completed.", projectName, version));
                                    }
                                }
                            } catch (final IntegrationException e) {
                                updateAssetWrapperWithError(assetWrapper, e.getMessage());
                                logger.error("Problem communicating with Black Duck: {}", e.getMessage());
                            } catch (final InterruptedException e) {
                                updateAssetWrapperWithError(assetWrapper, "Waiting for the scan to complete was interrupted: " + e.getMessage());
                                logger.error("Problem communicating with Black Duck: {}", e.getMessage());
                            }
                        }
                    }
                }
            }
        }
        if (phoneHomeResponse.isPresent()) {
            commonRepositoryTaskHelper.endPhoneHome(phoneHomeResponse.get());
        } else {
            logger.debug("Could not phone home.");
        }
    }

    private void updateAssetWrapperWithError(final AssetWrapper assetWrapper, final String message) {
        assetWrapper.removeAllBlackDuckData();
        assetWrapper.addFailureToBlackDuckPanel(message);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
        assetWrapper.updateAsset();
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

    private CodeLocationCreationData<ScanBatchOutput> performScan(final BlackDuckServerConfig blackDuckServerConfig, final File workingBlackDuckDirectory, final File outputDirectory, final File tempFileStorage,
        final String codeLocationName,
        final AssetWrapper assetWrapper, final SignatureScannerService signatureScannerService) {
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

        CodeLocationCreationData<ScanBatchOutput> scanData = null;
        try {
            final ScanBatch scanBatch = createScanBatch(blackDuckServerConfig, workingBlackDuckDirectory, outputDirectory, projectName, version, binaryFile.getAbsolutePath(), codeLocationName);
            scanData = signatureScannerService.performSignatureScan(scanBatch);

            if (scanData.getOutput().getSuccessfulCodeLocationNames().contains(codeLocationName)) {
                assetWrapper.addPendingToBlackDuckPanel("Scan uploaded to Black Duck, waiting for update.");
            }
        } catch (final IntegrationException e) {
            assetWrapper.removeAllBlackDuckData();
            assetWrapper.addFailureToBlackDuckPanel(e.getMessage());
            logger.error("Error scanning asset: {}. Reason: {}", name, e.getMessage());
        } finally {
            assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
        }
        return scanData;
    }

    private ScanBatch createScanBatch(final BlackDuckServerConfig blackDuckServerConfig, final File workingBlackDuckDirectory, final File outputDirectory, final String projectName, final String projectVersion, final String pathToScan,
        final String codeLocationName) {
        final int scanMemory = taskConfiguration().getInteger(ScanTaskDescriptor.KEY_SCAN_MEMORY, ScanTaskDescriptor.DEFAULT_SCAN_MEMORY);

        final ScanBatchBuilder scanBatchBuilder = new ScanBatchBuilder();
        scanBatchBuilder.fromBlackDuckServerConfig(blackDuckServerConfig);
        scanBatchBuilder.installDirectory(workingBlackDuckDirectory);
        scanBatchBuilder.outputDirectory(outputDirectory);
        scanBatchBuilder.projectAndVersionNames(projectName, projectVersion);
        scanBatchBuilder.addTarget(ScanTarget.createBasicTarget(pathToScan, codeLocationName));
        scanBatchBuilder.scanMemoryInMegabytes(scanMemory);

        return scanBatchBuilder.build();
    }
}
