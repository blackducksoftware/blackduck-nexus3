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
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationData;
import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationService;
import com.synopsys.integration.blackduck.codelocation.CodeLocationWaitResult;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatch;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatchBuilder;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatchOutput;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatchRunner;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.SignatureScannerService;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.command.ScanTarget;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.exception.BlackDuckApiException;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonTaskFilters;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.rest.BlackDuckHttpClient;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.phonehome.PhoneHomeResponse;
import com.synopsys.integration.util.IntEnvironmentVariables;
import com.synopsys.integration.util.NoThreadExecutorService;

@Named
public class ScanTask extends RepositoryTaskSupport {
    public static final String SCAN_CODE_LOCATION_NAME = "Nexus3Scan";
    private static final String BLACK_DUCK_COMMUNICATION_FORMAT = "Problem communicating with Black Duck: {}";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final QueryManager queryManager;
    private final DateTimeParser dateTimeParser;
    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final ScanMetaDataProcessor scanMetaDataProcessor;
    private final CommonTaskFilters commonTaskFilters;

    @Inject
    public ScanTask(QueryManager queryManager, DateTimeParser dateTimeParser, CommonRepositoryTaskHelper commonRepositoryTaskHelper, ScanMetaDataProcessor scanMetaDataProcessor,
        CommonTaskFilters commonTaskFilters) {
        this.queryManager = queryManager;
        this.dateTimeParser = dateTimeParser;
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.scanMetaDataProcessor = scanMetaDataProcessor;
        this.commonTaskFilters = commonTaskFilters;
    }

    @Override
    protected boolean appliesTo(Repository repository) {
        return commonTaskFilters.doesRepositoryApply(repository, getRepositoryField());
    }

    @Override
    public String getMessage() {
        return commonRepositoryTaskHelper.getTaskMessage(ScanTaskDescriptor.BLACK_DUCK_SCAN_TASK_NAME, getRepositoryField());
    }

    @Override
    protected void execute(Repository repository) {
        IntLogger intLogger = new Slf4jIntLogger(logger);

        String exceptionMessage = null;
        BlackDuckServerConfig blackDuckServerConfig = null;
        SignatureScannerService signatureScannerService = null;
        CodeLocationCreationService codeLocationCreationService = null;
        BlackDuckService blackDuckService = null;
        ProjectService projectService = null;
        Optional<PhoneHomeResponse> phoneHomeResponse = Optional.empty();
        try {
            blackDuckServerConfig = commonRepositoryTaskHelper.getBlackDuckServerConfig();
            BlackDuckServicesFactory blackDuckServicesFactory = commonRepositoryTaskHelper.getBlackDuckServicesFactory();

            IntEnvironmentVariables intEnvironmentVariables = new IntEnvironmentVariables();
            BlackDuckHttpClient blackDuckHttpClient = blackDuckServerConfig.createBlackDuckHttpClient(intLogger);

            signatureScannerService = blackDuckServicesFactory.createSignatureScannerService(ScanBatchRunner.createDefault(intLogger, blackDuckHttpClient, intEnvironmentVariables, new NoThreadExecutorService()));
            codeLocationCreationService = blackDuckServicesFactory.createCodeLocationCreationService();
            blackDuckService = blackDuckServicesFactory.createBlackDuckService();
            projectService = blackDuckServicesFactory.createProjectService();
            phoneHomeResponse = commonRepositoryTaskHelper.phoneHome(ScanTaskDescriptor.BLACK_DUCK_SCAN_TASK_ID);
        } catch (IntegrationException | IllegalStateException e) {
            logger.error(String.format("Black Duck hub server config invalid. %s", e.getMessage()), e);
            exceptionMessage = e.getMessage();
        }

        File workingDirectory = commonRepositoryTaskHelper.getWorkingDirectory(taskConfiguration());
        File workingBlackDuckDirectory = new File(workingDirectory, "blackduck");
        File tempFileStorage = new File(workingBlackDuckDirectory, "temp");
        File outputDirectory = new File(workingBlackDuckDirectory, "output");
        try {
            Files.createDirectories(tempFileStorage.toPath());
            Files.createDirectories(outputDirectory.toPath());
        } catch (IOException e) {
            intLogger.debug(e.getMessage(), e);
            throw new TaskInterruptedException("Could not create directories to use with Scanner: " + e.getMessage(), true);
        }

        boolean alwaysScan = taskConfiguration().getBoolean(ScanTaskDescriptor.KEY_ALWAYS_CHECK, false);
        boolean redoFailures = taskConfiguration().getBoolean(ScanTaskDescriptor.KEY_REDO_FAILURES, false);
        for (Repository foundRepository : commonTaskFilters.findRelevantRepositories(repository)) {
            if (commonTaskFilters.isHostedRepository(foundRepository.getType())) {
                String repoName = foundRepository.getName();
                logger.info("Checking repository for assets: {}", repoName);
                Query filteredQuery = commonRepositoryTaskHelper.createPagedQuery(Optional.empty()).build();
                PagedResult<Asset> foundAssets = commonRepositoryTaskHelper.retrievePagedAssets(foundRepository, filteredQuery);

                while (foundAssets.hasResults()) {
                    logger.debug("Found results from DB");
                    Map<Optional<CodeLocationCreationData<ScanBatchOutput>>, AssetWrapper> scannedAssets = new HashMap<>();
                    for (Asset asset : foundAssets.getTypeList()) {
                        AssetWrapper assetWrapper = AssetWrapper.createScanAssetWrapper(asset, foundRepository, queryManager);
                        String name = assetWrapper.getFullPath();
                        String version = assetWrapper.getVersion();
                        String codeLocationName = scanMetaDataProcessor.createCodeLocationName(repoName, name, version);

                        TaskStatus status = assetWrapper.getBlackDuckStatus();
                        boolean shouldScan = shouldScan(status, alwaysScan, redoFailures);
                        logger.debug("Status matches, {}", shouldScan);
                        boolean shouldScanAgain = commonTaskFilters.hasAssetBeenModified(assetWrapper);
                        logger.debug("Process again, {}", shouldScanAgain);
                        boolean scan = shouldScan || shouldScanAgain;
                        logger.debug("Scan without filter check, {}", scan);

                        DateTime lastModified = assetWrapper.getAssetLastUpdated();
                        String fullPathName = assetWrapper.getFullPath();
                        String fileName = null;
                        try {
                            fileName = assetWrapper.getFilename();
                        } catch (IntegrationException e) {
                            logger.debug("Skipping asset: {}. {}", name, e.getMessage());
                        }
                        if (commonTaskFilters.skipAssetProcessing(lastModified, fullPathName, fileName, taskConfiguration()) || !scan) {
                            logger.debug("Binary file did not meet requirements for scan: {}", name);
                            continue;
                        }

                        if (StringUtils.isNotBlank(exceptionMessage)) {
                            commonRepositoryTaskHelper.failedConnection(assetWrapper, exceptionMessage);
                            assetWrapper.updateAsset();
                        } else if (null != blackDuckServerConfig && null != signatureScannerService) {
                            Optional<CodeLocationCreationData<ScanBatchOutput>> scanData = performScan(blackDuckServerConfig, workingBlackDuckDirectory, outputDirectory, tempFileStorage, codeLocationName, assetWrapper,
                                signatureScannerService,
                                blackDuckService, projectService);
                            scannedAssets.put(scanData, assetWrapper);
                            assetWrapper.updateAsset();
                        }
                    }

                    try {
                        FileUtils.cleanDirectory(tempFileStorage);
                        FileUtils.cleanDirectory(outputDirectory);
                    } catch (IOException e) {
                        logger.warn("Problem cleaning scan directories {}", outputDirectory.getAbsolutePath());
                    }

                    Query nextPageQuery = commonRepositoryTaskHelper.createPagedQuery(foundAssets.getLastName()).build();
                    foundAssets = commonRepositoryTaskHelper.retrievePagedAssets(foundRepository, nextPageQuery);

                    if (null != signatureScannerService && null != projectService && null != blackDuckService) {
                        for (Map.Entry<Optional<CodeLocationCreationData<ScanBatchOutput>>, AssetWrapper> entry : scannedAssets.entrySet()) {
                            AssetWrapper assetWrapper = entry.getValue();
                            Optional<CodeLocationCreationData<ScanBatchOutput>> scanDataOptional = entry.getKey();
                            String projectName = assetWrapper.getName();
                            String version = assetWrapper.getVersion();
                            int timeout = blackDuckServerConfig.getTimeout() * 5;
                            try {
                                if (scanDataOptional.isPresent()) {
                                    CodeLocationCreationData<ScanBatchOutput> scanData = scanDataOptional.get();
                                    if (!scanData.getOutput().getSuccessfulCodeLocationNames().isEmpty()) {
                                        ProjectVersionView projectVersionView = scanMetaDataProcessor.getOrCreateProjectVersion(blackDuckService, projectService, projectName, version);
                                        Set<String> successfulCodeLocationNames = scanData.getOutput().getSuccessfulCodeLocationNames();
                                        CodeLocationWaitResult codeLocationWaitResult = codeLocationCreationService
                                                                                            .waitForCodeLocations(scanData.getNotificationTaskRange(), successfulCodeLocationNames, successfulCodeLocationNames.size(),
                                                                                                timeout);
                                        if (CodeLocationWaitResult.Status.COMPLETE == codeLocationWaitResult.getStatus()) {
                                            scanMetaDataProcessor
                                                .updateRepositoryMetaData(blackDuckService, assetWrapper, projectVersionView.getHref().orElse(blackDuckServerConfig.getBlackDuckUrl().toString()), projectVersionView);
                                        } else {
                                            updateAssetWrapperWithError(assetWrapper, String.format("The Black Duck server did not update this project within %s seconds", timeout));
                                        }
                                    }
                                }
                            } catch (IntegrationException e) {
                                updateAssetWrapperWithError(assetWrapper, e.getMessage());
                                logger.error(BLACK_DUCK_COMMUNICATION_FORMAT, e.getMessage());
                            } catch (InterruptedException e) {
                                String errorMessage = "Waiting for the scan to complete was interrupted: " + e.getMessage();
                                updateAssetWrapperWithError(assetWrapper, errorMessage);
                                logger.error(errorMessage);
                                Thread.currentThread().interrupt();
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

    private void updateAssetWrapperWithError(AssetWrapper assetWrapper, String message) {
        assetWrapper.removeAllBlackDuckData();
        assetWrapper.addFailureToBlackDuckPanel(message);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
        assetWrapper.updateAsset();
    }

    private boolean shouldScan(TaskStatus status, boolean alwaysScan, boolean redoFailures) {
        if (TaskStatus.PENDING.equals(status) || TaskStatus.SUCCESS.equals(status)) {
            return alwaysScan;
        }

        if (TaskStatus.FAILURE.equals(status)) {
            return redoFailures;
        }

        return true;
    }

    private Optional<CodeLocationCreationData<ScanBatchOutput>> performScan(BlackDuckServerConfig blackDuckServerConfig, File workingBlackDuckDirectory, File outputDirectory, File tempFileStorage,
        String codeLocationName, AssetWrapper assetWrapper, SignatureScannerService signatureScannerService, BlackDuckService blackDuckService, ProjectService projectService) {
        String name = assetWrapper.getFullPath();
        String projectName = assetWrapper.getName();
        String version = assetWrapper.getVersion();

        logger.info("Scanning item: {}", name);
        File binaryFile;
        try {
            binaryFile = assetWrapper.getBinaryBlobFile(tempFileStorage);
        } catch (IntegrationException e) {
            String errorMessage = String.format("Could not scan item: %s. %s.", name, e.getMessage());
            logger.warn(errorMessage);
            updateAssetWrapperWithError(assetWrapper, errorMessage);
            return Optional.empty();
        } catch (IOException e) {
            logger.debug("Exception thrown: {}", e.getMessage());
            throw new TaskInterruptedException("Error saving blob binary to file", true);
        }

        CodeLocationCreationData<ScanBatchOutput> scanData = null;
        try {
            ScanBatch scanBatch = createScanBatch(blackDuckServerConfig, workingBlackDuckDirectory, outputDirectory, projectName, version, binaryFile.getAbsolutePath(), codeLocationName);
            scanData = signatureScannerService.performSignatureScan(scanBatch);
            scanMetaDataProcessor.getOrCreateProjectVersion(blackDuckService, projectService, projectName, version);
            if (scanData.getOutput().getSuccessfulCodeLocationNames().contains(codeLocationName)) {
                assetWrapper.addPendingToBlackDuckPanel("Scan uploaded to Black Duck, waiting for update.");
            }
        } catch (BlackDuckApiException e) {
            assetWrapper.removeAllBlackDuckData();
            assetWrapper.addFailureToBlackDuckPanel(e.getMessage());
            logger.error(BLACK_DUCK_COMMUNICATION_FORMAT, e.getMessage());
        } catch (IntegrationException e) {
            assetWrapper.removeAllBlackDuckData();
            assetWrapper.addFailureToBlackDuckPanel(e.getMessage());
            logger.error("Error scanning asset: {}. Reason: {}", name, e.getMessage());
        } finally {
            assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
        }
        return Optional.ofNullable(scanData);
    }

    private ScanBatch createScanBatch(BlackDuckServerConfig blackDuckServerConfig, File workingBlackDuckDirectory, File outputDirectory, String projectName, String projectVersion, String pathToScan,
        String codeLocationName) {
        int scanMemory = taskConfiguration().getInteger(ScanTaskDescriptor.KEY_SCAN_MEMORY, ScanTaskDescriptor.DEFAULT_SCAN_MEMORY);

        ScanBatchBuilder scanBatchBuilder = new ScanBatchBuilder();
        scanBatchBuilder.fromBlackDuckServerConfig(blackDuckServerConfig);
        scanBatchBuilder.installDirectory(workingBlackDuckDirectory);
        scanBatchBuilder.outputDirectory(outputDirectory);
        scanBatchBuilder.projectAndVersionNames(projectName, projectVersion);
        scanBatchBuilder.addTarget(ScanTarget.createBasicTarget(pathToScan, codeLocationName));
        scanBatchBuilder.scanMemoryInMegabytes(scanMemory);

        return scanBatchBuilder.build();
    }
}
