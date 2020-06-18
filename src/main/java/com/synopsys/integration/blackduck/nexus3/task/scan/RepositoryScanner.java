package com.synopsys.integration.blackduck.nexus3.task.scan;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationData;
import com.synopsys.integration.blackduck.codelocation.CodeLocationWaitResult;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatch;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatchBuilder;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatchOutput;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.command.ScanTarget;
import com.synopsys.integration.blackduck.exception.BlackDuckApiException;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonTaskFilters;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.util.NameVersion;

public class RepositoryScanner {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String BLACK_DUCK_COMMUNICATION_FORMAT = "Problem communicating with Black Duck: %s.";

    private final QueryManager queryManager;
    private final DateTimeParser dateTimeParser;
    private final ScanMetaDataProcessor scanMetaDataProcessor;
    private final TaskConfiguration taskConfiguration;
    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final CommonTaskFilters commonTaskFilters;

    private final ScanConfiguration scanConfiguration;

    public RepositoryScanner(QueryManager queryManager, DateTimeParser dateTimeParser, ScanMetaDataProcessor scanMetaDataProcessor, TaskConfiguration taskConfiguration,
        CommonRepositoryTaskHelper commonRepositoryTaskHelper, CommonTaskFilters commonTaskFilters, ScanConfiguration scanConfiguration) {
        this.queryManager = queryManager;
        this.dateTimeParser = dateTimeParser;
        this.scanMetaDataProcessor = scanMetaDataProcessor;
        this.taskConfiguration = taskConfiguration;
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.commonTaskFilters = commonTaskFilters;
        this.scanConfiguration = scanConfiguration;
    }

    public void scanRepository() {
        String repoName = scanConfiguration.getRepository().getName();
        logger.info("Checking repository for assets: {}", repoName);
        Query filteredQuery = commonRepositoryTaskHelper.createPagedQuery(Optional.empty()).build();
        PagedResult<Asset> foundAssets = commonRepositoryTaskHelper.retrievePagedAssets(scanConfiguration.getRepository(), filteredQuery);

        while (foundAssets.hasResults()) {
            logger.debug("Found results from DB");
            Map<AssetWrapper, Optional<CodeLocationCreationData<ScanBatchOutput>>> scannedAssets = new HashMap<>();
            for (Asset asset : foundAssets.getTypeList()) {
                scanAsset(asset, repoName, scannedAssets);
            }
            try {
                FileUtils.cleanDirectory(scanConfiguration.getTempFileStorage());
                FileUtils.cleanDirectory(scanConfiguration.getOutputDirectory());
            } catch (IOException e) {
                logger.warn("Problem cleaning scan directories {}", scanConfiguration.getOutputDirectory().getAbsolutePath());
                logger.debug(e.getMessage(), e);
            }
            logger.error("Scanned assets : " + scannedAssets.size());
            if (!scanConfiguration.hasErrors()) {
                for (Map.Entry<AssetWrapper, Optional<CodeLocationCreationData<ScanBatchOutput>>> entry : scannedAssets.entrySet()) {
                    processScannedAsset(entry.getKey(), entry.getValue());
                }
            } else {
                logger.error("Scan Configuration has errors");
            }
            Query nextPageQuery = commonRepositoryTaskHelper.createPagedQuery(foundAssets.getLastName()).build();
            foundAssets = commonRepositoryTaskHelper.retrievePagedAssets(scanConfiguration.getRepository(), nextPageQuery);
        }

    }

    private void scanAsset(Asset asset, String repoName, Map<AssetWrapper, Optional<CodeLocationCreationData<ScanBatchOutput>>> scannedAssets) {
        AssetWrapper assetWrapper = AssetWrapper.createScanAssetWrapper(asset, scanConfiguration.getRepository(), queryManager);
        String name = assetWrapper.getFullPath();
        logger.debug("Processing asset: {}", name);
        String version = assetWrapper.getVersion();
        String codeLocationName = scanMetaDataProcessor.createCodeLocationName(repoName, name, version);

        TaskStatus status = assetWrapper.getBlackDuckStatus();
        boolean shouldScan = shouldScan(status);
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
            logger.debug(String.format("Skipping asset: %s. %s", name, e.getMessage()), e);
        }
        if (commonTaskFilters.skipAssetProcessing(lastModified, fullPathName, fileName, taskConfiguration) || !scan) {
            logger.debug("Binary file did not meet requirements for scan: {}", name);
            return;
        }

        if (scanConfiguration.hasErrors()) {
            commonRepositoryTaskHelper.failedConnection(assetWrapper, scanConfiguration.getExceptionMessage());
            assetWrapper.updateAsset();
        } else {
            Optional<CodeLocationCreationData<ScanBatchOutput>> scanData = performScan(codeLocationName, assetWrapper);
            scannedAssets.put(assetWrapper, scanData);
            assetWrapper.updateAsset();
        }
    }

    private void processScannedAsset(AssetWrapper assetWrapper, Optional<CodeLocationCreationData<ScanBatchOutput>> scanDataOptional) {
        String projectName = assetWrapper.getName();
        String version = assetWrapper.getVersion();
        NameVersion projectNameVersion = new NameVersion(projectName, version);
        int timeout = scanConfiguration.getBlackDuckServerConfig().getTimeout() * 5;
        try {
            if (scanDataOptional.isPresent()) {
                CodeLocationCreationData<ScanBatchOutput> scanData = scanDataOptional.get();
                if (!scanData.getOutput().getSuccessfulCodeLocationNames().isEmpty()) {
                    ProjectVersionView projectVersionView = scanMetaDataProcessor.getOrCreateProjectVersion(scanConfiguration.getBlackDuckService(), scanConfiguration.getProjectService(), projectName, version);
                    Set<String> successfulCodeLocationNames = scanData.getOutput().getSuccessfulCodeLocationNames();
                    CodeLocationWaitResult codeLocationWaitResult = scanConfiguration.getCodeLocationCreationService()
                                                                        .waitForCodeLocations(scanData.getNotificationTaskRange(), projectNameVersion, successfulCodeLocationNames, successfulCodeLocationNames.size(), timeout);
                    if (CodeLocationWaitResult.Status.COMPLETE == codeLocationWaitResult.getStatus()) {
                        scanMetaDataProcessor
                            .updateRepositoryMetaData(scanConfiguration.getBlackDuckService(), assetWrapper, projectVersionView.getHref().orElse(scanConfiguration.getBlackDuckServerConfig().getBlackDuckUrl().toString()),
                                projectVersionView);
                    } else {
                        updateAssetWrapperWithError(assetWrapper, String.format("The Black Duck server did not update this project within %s seconds", timeout));
                    }
                }
            }
        } catch (IntegrationException e) {
            updateAssetWrapperWithError(assetWrapper, e.getMessage());
            logger.error(String.format(BLACK_DUCK_COMMUNICATION_FORMAT, e.getMessage()));
            logger.debug(e.getMessage(), e);
        } catch (InterruptedException e) {
            String errorMessage = "Waiting for the scan to complete was interrupted: " + e.getMessage();
            updateAssetWrapperWithError(assetWrapper, errorMessage);
            logger.error(errorMessage);
            logger.debug(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    private void updateAssetWrapperWithError(AssetWrapper assetWrapper, String message) {
        assetWrapper.removeAllBlackDuckData();
        assetWrapper.addFailureToBlackDuckPanel(message);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
        assetWrapper.updateAsset();
    }

    private boolean shouldScan(TaskStatus status) {
        if (TaskStatus.PENDING.equals(status) || TaskStatus.SUCCESS.equals(status)) {
            return scanConfiguration.isAlwaysScan();
        }
        if (TaskStatus.FAILURE.equals(status)) {
            return scanConfiguration.isRedoFailures();
        }
        return true;
    }

    private Optional<CodeLocationCreationData<ScanBatchOutput>> performScan(String codeLocationName, AssetWrapper assetWrapper) {
        String fullPath = assetWrapper.getFullPath();
        String projectName = assetWrapper.getName();
        String version = assetWrapper.getVersion();

        logger.info("Scanning item: {}, version: {}, path: {}", projectName, version, fullPath);
        File binaryFile;
        try {
            binaryFile = assetWrapper.getBinaryBlobFile(scanConfiguration.getTempFileStorage());
        } catch (IntegrationException e) {
            String errorMessage = String.format("Could not scan item: %s. %s.", fullPath, e.getMessage());
            logger.warn(errorMessage);
            logger.debug(e.getMessage(), e);
            updateAssetWrapperWithError(assetWrapper, errorMessage);
            return Optional.empty();
        } catch (IOException e) {
            logger.debug(String.format("Exception thrown: %s", e.getMessage()), e);
            throw new TaskInterruptedException("Error saving blob binary to file", true);
        }
        CodeLocationCreationData<ScanBatchOutput> scanData = null;
        try {
            ScanBatch scanBatch = createScanBatch(projectName, version, binaryFile.getAbsolutePath(), codeLocationName);
            scanData = scanConfiguration.getSignatureScannerService().performSignatureScan(scanBatch);
            scanMetaDataProcessor.getOrCreateProjectVersion(scanConfiguration.getBlackDuckService(), scanConfiguration.getProjectService(), projectName, version);
            if (scanData.getOutput().getSuccessfulCodeLocationNames().contains(codeLocationName)) {
                assetWrapper.addPendingToBlackDuckPanel("Scan uploaded to Black Duck, waiting for update.");
            }
        } catch (BlackDuckApiException e) {
            String errorMessage = String.format(BLACK_DUCK_COMMUNICATION_FORMAT, e.getMessage());
            handleScanException(assetWrapper, errorMessage, e);
        } catch (IntegrationException | IllegalArgumentException e) {
            String errorMessage = String.format("Error scanning asset: %s, version: %s, path: %s. Reason: %s", projectName, version, fullPath, e.getMessage());
            handleScanException(assetWrapper, errorMessage, e);
        } finally {
            assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
        }
        return Optional.ofNullable(scanData);
    }

    private void handleScanException(AssetWrapper assetWrapper, String errorMessage, Exception exception) {
        logger.error(errorMessage);
        logger.debug(exception.getMessage(), exception);
        assetWrapper.removeAllBlackDuckData();
        assetWrapper.addFailureToBlackDuckPanel(errorMessage);
    }

    private ScanBatch createScanBatch(String projectName, String projectVersion, String pathToScan, String codeLocationName) {
        int scanMemory = taskConfiguration.getInteger(ScanTaskDescriptor.KEY_SCAN_MEMORY, ScanTaskDescriptor.DEFAULT_SCAN_MEMORY);

        ScanBatchBuilder scanBatchBuilder = new ScanBatchBuilder();
        scanBatchBuilder.fromBlackDuckServerConfig(scanConfiguration.getBlackDuckServerConfig());
        scanBatchBuilder.installDirectory(scanConfiguration.getWorkingBlackDuckDirectory());
        scanBatchBuilder.outputDirectory(scanConfiguration.getOutputDirectory());
        scanBatchBuilder.projectAndVersionNames(projectName, projectVersion);
        scanBatchBuilder.addTarget(ScanTarget.createBasicTarget(pathToScan, codeLocationName));
        scanBatchBuilder.scanMemoryInMegabytes(scanMemory);

        return scanBatchBuilder.build();
    }
}
