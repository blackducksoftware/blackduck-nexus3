package com.synopsys.integration.blackduck.nexus3.task.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.codelocation.CodeLocationWaitResult;
import com.synopsys.integration.blackduck.exception.BlackDuckApiException;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonMetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.inspector.InspectorMetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.task.scan.ScanMetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.model.NotificationTaskRange;
import com.synopsys.integration.exception.IntegrationException;

public class MetadataRepositoryScanner {
    private static final String BLACK_DUCK_COMMUNICATION_FORMAT = "Problem communicating with Black Duck: {}";
    private static final String METADATA_CHECK_ERROR = "Problem checking metadata: ";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final QueryManager queryManager;
    private final CommonMetaDataProcessor commonMetaDataProcessor;
    private final InspectorMetaDataProcessor inspectorMetaDataProcessor;
    private final ScanMetaDataProcessor scanMetaDataProcessor;
    private final DateTimeParser dateTimeParser;

    private final MetaDataScanConfiguration metaDataScanConfiguration;

    public MetadataRepositoryScanner(CommonRepositoryTaskHelper commonRepositoryTaskHelper, QueryManager queryManager, CommonMetaDataProcessor commonMetaDataProcessor,
        InspectorMetaDataProcessor inspectorMetaDataProcessor, ScanMetaDataProcessor scanMetaDataProcessor, DateTimeParser dateTimeParser, MetaDataScanConfiguration metaDataScanConfiguration) {
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.queryManager = queryManager;
        this.commonMetaDataProcessor = commonMetaDataProcessor;
        this.inspectorMetaDataProcessor = inspectorMetaDataProcessor;
        this.scanMetaDataProcessor = scanMetaDataProcessor;
        this.dateTimeParser = dateTimeParser;
        this.metaDataScanConfiguration = metaDataScanConfiguration;
    }

    public void scanRepository() {
        String repoName = metaDataScanConfiguration.getRepository().getName();
        Query filteredAssets = createFilteredQuery(Optional.empty());
        PagedResult<Asset> pagedAssets = commonRepositoryTaskHelper.retrievePagedAssets(metaDataScanConfiguration.getRepository(), filteredAssets);
        Map<String, AssetWrapper> assetWrapperMap = new HashMap<>();
        Map<String, AssetWrapper> assetWrapperToWaitFor = new HashMap<>();
        while (pagedAssets.hasResults()) {
            logger.debug("Found items in the DB.");
            for (Asset asset : pagedAssets.getTypeList()) {
                updateAsset(asset, repoName, assetWrapperToWaitFor, assetWrapperMap);
            }
            Query nextPage = createFilteredQuery(pagedAssets.getLastName());
            pagedAssets = commonRepositoryTaskHelper.retrievePagedAssets(metaDataScanConfiguration.getRepository(), nextPage);
        }

        if (!assetWrapperToWaitFor.isEmpty() && !metaDataScanConfiguration.hasErrors()) {
            updatePendingScanAssets(assetWrapperToWaitFor);
        }

        if (metaDataScanConfiguration.isProxyRepo() && !metaDataScanConfiguration.hasErrors()) {
            logger.info("Updating data of proxy repository.");
            updateProxyAssets(repoName, assetWrapperMap);
        }
    }

    private void updateAsset(Asset asset, String repoName, Map<String, AssetWrapper> assetWrapperToWaitFor, Map<String, AssetWrapper> assetWrapperMap) {
        AssetWrapper assetWrapper = AssetWrapper.createAssetWrapper(asset, metaDataScanConfiguration.getRepository(), queryManager, metaDataScanConfiguration.getAssetStatusLabel());
        if (metaDataScanConfiguration.hasErrors()) {
            commonRepositoryTaskHelper.failedConnection(assetWrapper, metaDataScanConfiguration.getExceptionMessage());
            assetWrapper.updateAsset();
        } else {
            updateAsset(assetWrapper, repoName, assetWrapperToWaitFor, assetWrapperMap);
        }
    }

    private void updateProxyAssets(String repoName, Map<String, AssetWrapper> assetWrapperMap) {
        try {
            String blackDuckUrl = commonRepositoryTaskHelper.getBlackDuckServerConfig().getBlackDuckUrl().toString();
            ProjectVersionView projectVersionView = inspectorMetaDataProcessor.getOrCreateProjectVersion(metaDataScanConfiguration.getBlackDuckService(), metaDataScanConfiguration.getProjectService(), repoName);
            inspectorMetaDataProcessor.updateRepositoryMetaData(metaDataScanConfiguration.getBlackDuckService(), blackDuckUrl, projectVersionView, assetWrapperMap, TaskStatus.SUCCESS);
        } catch (BlackDuckApiException e) {
            for (Map.Entry<String, AssetWrapper> entry : assetWrapperMap.entrySet()) {
                updateAssetWrapperWithError(entry.getValue(), e.getMessage());
                logger.error(BLACK_DUCK_COMMUNICATION_FORMAT, e.getMessage());
            }
        } catch (IntegrationException e) {
            for (Map.Entry<String, AssetWrapper> entry : assetWrapperMap.entrySet()) {
                updateAssetWrapperWithError(entry.getValue(), String.format("Problem retrieving the project %s from Hub: %s", repoName, e.getMessage()));
                logger.error(BLACK_DUCK_COMMUNICATION_FORMAT, e.getMessage());
            }
            throw new TaskInterruptedException("Problem retrieving project from Hub: " + e.getMessage(), true);
        }
    }

    private void updateAsset(AssetWrapper assetWrapper, String repoName, Map<String, AssetWrapper> assetWrapperToWaitFor, Map<String, AssetWrapper> assetWrapperMap) {
        String assetName = assetWrapper.getName();
        logger.info("Updating metadata for {}", assetName);
        try {
            if (!metaDataScanConfiguration.isProxyRepo()) {
                String assetBlackDuckUrl = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.BLACKDUCK_URL);
                TaskStatus status = assetWrapper.getBlackDuckStatus();
                String lastProcessedString = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME);
                DateTime lastProcessed = dateTimeParser.convertFromStringToDate(lastProcessedString);
                String version = assetWrapper.getVersion();

                if (StringUtils.isBlank(assetBlackDuckUrl) && isPendingOrComponentNotFoundForDay(status, lastProcessed)) {
                    String codeLocationName = scanMetaDataProcessor.createCodeLocationName(repoName, assetName, version);
                    logger.info("Re-checking code location {}", codeLocationName);
                    assetWrapperToWaitFor.put(codeLocationName, assetWrapper);
                } else if (!metaDataScanConfiguration.hasErrors()) {
                    ProjectVersionView projectVersionView = commonMetaDataProcessor.getOrCreateProjectVersion(metaDataScanConfiguration.getBlackDuckService(), metaDataScanConfiguration.getProjectService(), assetName, version);
                    logger.info("Updating data of hosted repository.");
                    scanMetaDataProcessor.updateRepositoryMetaData(metaDataScanConfiguration.getBlackDuckService(), assetWrapper, projectVersionView.getHref().orElse(assetBlackDuckUrl), projectVersionView);

                }
            } else {
                String originId = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.ASSET_ORIGIN_ID);
                assetWrapperMap.put(originId, assetWrapper);
            }
        } catch (BlackDuckApiException e) {
            updateAssetWrapperWithError(assetWrapper, e.getMessage());
            logger.error(BLACK_DUCK_COMMUNICATION_FORMAT, e.getMessage());
        } catch (IntegrationException e) {
            updateAssetWrapperWithError(assetWrapper, e.getMessage());
            throw new TaskInterruptedException(METADATA_CHECK_ERROR + e.getMessage(), true);
        }
    }

    private void updatePendingScanAssets(Map<String, AssetWrapper> assetWrapperToWaitFor) {
        NotificationTaskRange notificationTaskRange = null;
        CodeLocationWaitResult codeLocationWaitResult = null;
        String errorMessage = null;
        int timeout = -1;
        try {
            timeout = commonRepositoryTaskHelper.getBlackDuckServerConfig().getTimeout() * 5;
            notificationTaskRange = metaDataScanConfiguration.getCodeLocationCreationService().calculateCodeLocationRange();
            Set<String> codeLocationNames = assetWrapperToWaitFor.keySet();
            codeLocationWaitResult = metaDataScanConfiguration.getCodeLocationCreationService().waitForCodeLocations(notificationTaskRange, codeLocationNames, codeLocationNames.size(), timeout);
        } catch (InterruptedException e) {
            errorMessage = "Waiting for the scan to complete was interrupted: " + e.getMessage();
            logger.error(errorMessage);
            Thread.currentThread().interrupt();
        } catch (BlackDuckApiException e) {
            errorMessage = e.getMessage();
            logger.error(BLACK_DUCK_COMMUNICATION_FORMAT, errorMessage);
        } catch (IntegrationException e) {
            errorMessage = e.getMessage();
            throw new TaskInterruptedException(METADATA_CHECK_ERROR + errorMessage, true);
        }
        for (Map.Entry<String, AssetWrapper> entry : assetWrapperToWaitFor.entrySet()) {
            String codeLocationName = entry.getKey();
            AssetWrapper assetWrapper = entry.getValue();

            if (StringUtils.isBlank(errorMessage) && null != codeLocationWaitResult && !codeLocationWaitResult.getCodeLocationNames().contains(codeLocationName)) {
                errorMessage = String.format("The Black Duck server did not update this project within %s seconds", timeout);
            }
            if (StringUtils.isNotBlank(errorMessage)) {
                updateAssetWrapperWithError(assetWrapper, errorMessage);
                continue;
            }
            String name = assetWrapper.getName();
            String version = assetWrapper.getVersion();
            String assetBlackDuckUrl = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.BLACKDUCK_URL);
            try {
                ProjectVersionView projectVersionView = commonMetaDataProcessor.getOrCreateProjectVersion(metaDataScanConfiguration.getBlackDuckService(), metaDataScanConfiguration.getProjectService(), name, version);
                scanMetaDataProcessor.updateRepositoryMetaData(metaDataScanConfiguration.getBlackDuckService(), assetWrapper, projectVersionView.getHref().orElse(assetBlackDuckUrl), projectVersionView);
            } catch (BlackDuckApiException e) {
                updateAssetWrapperWithError(assetWrapper, e.getMessage());
                logger.error(BLACK_DUCK_COMMUNICATION_FORMAT, e.getMessage());
            } catch (IntegrationException e) {
                updateAssetWrapperWithError(assetWrapper, e.getMessage());
                throw new TaskInterruptedException(METADATA_CHECK_ERROR + e.getMessage(), true);
            }
        }

    }

    private void updateAssetWrapperWithError(AssetWrapper assetWrapper, String message) {
        commonMetaDataProcessor.removeAllMetaData(assetWrapper);
        assetWrapper.addFailureToBlackDuckPanel(message);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
        assetWrapper.updateAsset();
    }

    private Query createFilteredQuery(Optional<String> lastNameUsed) {
        Query.Builder pagedQueryBuilder = commonRepositoryTaskHelper.createPagedQuery(lastNameUsed);
        String statusPath = commonRepositoryTaskHelper.getBlackDuckPanelPath(metaDataScanConfiguration.getAssetStatusLabel());
        String oldStatusPath = commonRepositoryTaskHelper.getBlackDuckPanelPath(AssetPanelLabel.OLD_STATUS);
        pagedQueryBuilder.and(statusWhereStatement(statusPath)).or(statusWhereStatement(oldStatusPath));
        return pagedQueryBuilder.build();
    }

    private String statusWhereStatement(String blackDuckDbPath) {
        StringBuilder statusWhere = new StringBuilder();

        statusWhere.append("(");
        statusWhere.append(createEqualsStatement(blackDuckDbPath, TaskStatus.SUCCESS.name()));
        statusWhere.append(" OR ");
        statusWhere.append(createEqualsStatement(blackDuckDbPath, TaskStatus.PENDING.name()));
        statusWhere.append(" OR ");
        statusWhere.append(createEqualsStatement(blackDuckDbPath, TaskStatus.COMPONENT_NOT_FOUND.name()));
        statusWhere.append(")");

        return statusWhere.toString();
    }

    private String createEqualsStatement(String object, String value) {
        StringBuilder equalsStatement = new StringBuilder();
        equalsStatement.append(object);
        equalsStatement.append(" = '");
        equalsStatement.append(value);
        equalsStatement.append("'");
        return equalsStatement.toString();
    }

    private boolean isPendingOrComponentNotFoundForDay(TaskStatus status, DateTime lastProcessed) {
        if (TaskStatus.PENDING.equals(status) || TaskStatus.COMPONENT_NOT_FOUND.equals(status)) {
            String timeNow = dateTimeParser.getCurrentDateTime();
            DateTime now = dateTimeParser.convertFromStringToDate(timeNow);
            return now.isAfter(lastProcessed.plusDays(1));
        }

        return false;
    }
}
