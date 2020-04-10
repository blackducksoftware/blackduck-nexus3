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
package com.synopsys.integration.blackduck.nexus3.task.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationService;
import com.synopsys.integration.blackduck.codelocation.CodeLocationWaitResult;
import com.synopsys.integration.blackduck.exception.BlackDuckApiException;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonMetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonTaskFilters;
import com.synopsys.integration.blackduck.nexus3.task.inspector.InspectorMetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.task.scan.ScanMetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.NotificationTaskRange;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.phonehome.PhoneHomeResponse;

@Named
public class MetaDataTask extends RepositoryTaskSupport {
    private static final String BLACK_DUCK_COMMUNICATION_FORMAT = "Problem communicating with Black Duck: {}";
    private static final String METADATA_CHECK_ERROR = "Problem checking metadata: ";
    private final Logger logger = createLogger();
    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final QueryManager queryManager;
    private final CommonMetaDataProcessor commonMetaDataProcessor;
    private final InspectorMetaDataProcessor inspectorMetaDataProcessor;
    private final ScanMetaDataProcessor scanMetaDataProcessor;
    private final DateTimeParser dateTimeParser;
    private final CommonTaskFilters commonTaskFilters;

    @Inject
    public MetaDataTask(CommonRepositoryTaskHelper commonRepositoryTaskHelper, QueryManager queryManager, CommonMetaDataProcessor commonMetaDataProcessor, InspectorMetaDataProcessor inspectorMetaDataProcessor,
        ScanMetaDataProcessor scanMetaDataProcessor, DateTimeParser dateTimeParser, CommonTaskFilters commonTaskFilters) {
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.queryManager = queryManager;
        this.commonMetaDataProcessor = commonMetaDataProcessor;
        this.inspectorMetaDataProcessor = inspectorMetaDataProcessor;
        this.scanMetaDataProcessor = scanMetaDataProcessor;
        this.dateTimeParser = dateTimeParser;
        this.commonTaskFilters = commonTaskFilters;
    }

    @Override
    protected void execute(Repository repository) {
        String exceptionMessage = null;
        String blackDuckUrl = null;
        CodeLocationCreationService codeLocationCreationService = null;
        BlackDuckService blackDuckService = null;
        ProjectService projectService = null;
        Optional<PhoneHomeResponse> phoneHomeResponse = Optional.empty();
        try {
            blackDuckUrl = commonRepositoryTaskHelper.getBlackDuckServerConfig().getBlackDuckUrl().toString();
            BlackDuckServicesFactory blackDuckServicesFactory = commonRepositoryTaskHelper.getBlackDuckServicesFactory();
            codeLocationCreationService = blackDuckServicesFactory.createCodeLocationCreationService();
            blackDuckService = blackDuckServicesFactory.createBlackDuckService();
            projectService = blackDuckServicesFactory.createProjectService();
            phoneHomeResponse = commonRepositoryTaskHelper.phoneHome(MetaDataTaskDescriptor.BLACK_DUCK_META_DATA_TASK_ID);
        } catch (IntegrationException | IllegalStateException e) {
            logger.error("Black Duck hub server config invalid. " + e.getMessage(), e);
            exceptionMessage = e.getMessage();
        }
        for (Repository foundRepository : commonTaskFilters.findRelevantRepositories(repository)) {
            String repoName = foundRepository.getName();
            boolean isProxyRepo = commonTaskFilters.isProxyRepository(foundRepository.getType());
            logger.info("Checking repository for assets: {}", repoName);

            AssetPanelLabel assetStatusLabel;
            if (isProxyRepo) {
                assetStatusLabel = AssetPanelLabel.INSPECTION_TASK_STATUS;
            } else {
                assetStatusLabel = AssetPanelLabel.SCAN_TASK_STATUS;
            }

            Query filteredAssets = createFilteredQuery(assetStatusLabel, Optional.empty());
            PagedResult<Asset> pagedAssets = commonRepositoryTaskHelper.retrievePagedAssets(foundRepository, filteredAssets);
            Map<String, AssetWrapper> assetWrapperMap = new HashMap<>();
            Map<String, AssetWrapper> assetWrapperToWaitFor = new HashMap<>();
            while (pagedAssets.hasResults()) {
                logger.debug("Found items in the DB.");
                for (Asset asset : pagedAssets.getTypeList()) {
                    AssetWrapper assetWrapper = AssetWrapper.createAssetWrapper(asset, foundRepository, queryManager, assetStatusLabel);
                    if (StringUtils.isNotBlank(exceptionMessage)) {
                        commonRepositoryTaskHelper.failedConnection(assetWrapper, exceptionMessage);
                        assetWrapper.updateAsset();
                    } else {
                        String name = assetWrapper.getName();
                        logger.info("Updating metadata for {}", name);
                        try {
                            if (!isProxyRepo) {
                                String assetBlackDuckUrl = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.BLACKDUCK_URL);
                                TaskStatus status = assetWrapper.getBlackDuckStatus();
                                String lastProcessedString = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME);
                                DateTime lastProcessed = dateTimeParser.convertFromStringToDate(lastProcessedString);
                                String version = assetWrapper.getVersion();

                                if (StringUtils.isBlank(assetBlackDuckUrl) && isPendingOrComponentNotFoundForDay(status, lastProcessed)) {
                                    String codeLocationName = scanMetaDataProcessor.createCodeLocationName(repoName, name, version);
                                    logger.info("Re-checking code location {}", codeLocationName);
                                    assetWrapperToWaitFor.put(codeLocationName, assetWrapper);
                                } else {
                                    if (null != projectService && null != blackDuckService) {
                                        ProjectVersionView projectVersionView = commonMetaDataProcessor.getOrCreateProjectVersion(blackDuckService, projectService, name, version);
                                        logger.info("Updating data of hosted repository.");
                                        scanMetaDataProcessor.updateRepositoryMetaData(blackDuckService, assetWrapper, projectVersionView.getHref().orElse(assetBlackDuckUrl), projectVersionView);
                                    }
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
                }
                if (!assetWrapperToWaitFor.isEmpty() && null != codeLocationCreationService && null != projectService && null != blackDuckService) {
                    updatePendingScanAssets(assetWrapperToWaitFor, codeLocationCreationService, projectService, blackDuckService);
                }

                Query nextPage = createFilteredQuery(assetStatusLabel, pagedAssets.getLastName());
                pagedAssets = commonRepositoryTaskHelper.retrievePagedAssets(foundRepository, nextPage);
            }

            if (isProxyRepo && null != projectService && null != blackDuckService) {
                logger.info("Updating data of proxy repository.");
                try {
                    ProjectVersionView projectVersionView = inspectorMetaDataProcessor.getOrCreateProjectVersion(blackDuckService, projectService, repoName);
                    inspectorMetaDataProcessor.updateRepositoryMetaData(blackDuckService, blackDuckUrl, projectVersionView, assetWrapperMap, TaskStatus.SUCCESS);
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
        }
        if (phoneHomeResponse.isPresent()) {
            commonRepositoryTaskHelper.endPhoneHome(phoneHomeResponse.get());
        } else {
            logger.debug("Could not phone home.");
        }
    }

    private void updatePendingScanAssets(Map<String, AssetWrapper> assetWrapperToWaitFor, CodeLocationCreationService codeLocationCreationService, ProjectService projectService, BlackDuckService blackDuckService) {
        NotificationTaskRange notificationTaskRange = null;
        CodeLocationWaitResult codeLocationWaitResult = null;
        String errorMessage = null;
        int timeout = -1;
        try {
            timeout = commonRepositoryTaskHelper.getBlackDuckServerConfig().getTimeout() * 5;
            notificationTaskRange = codeLocationCreationService.calculateCodeLocationRange();
            Set<String> codeLocationNames = assetWrapperToWaitFor.keySet();
            codeLocationWaitResult = codeLocationCreationService.waitForCodeLocations(notificationTaskRange, codeLocationNames, codeLocationNames.size(), timeout);
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
                ProjectVersionView projectVersionView = commonMetaDataProcessor.getOrCreateProjectVersion(blackDuckService, projectService, name, version);
                scanMetaDataProcessor.updateRepositoryMetaData(blackDuckService, assetWrapper, projectVersionView.getHref().orElse(assetBlackDuckUrl), projectVersionView);
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

    private Query createFilteredQuery(AssetPanelLabel statusLabel, Optional<String> lastNameUsed) {
        Query.Builder pagedQueryBuilder = commonRepositoryTaskHelper.createPagedQuery(lastNameUsed);
        String statusPath = commonRepositoryTaskHelper.getBlackDuckPanelPath(statusLabel);
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

    @Override
    protected boolean appliesTo(Repository repository) {
        return commonTaskFilters.doesRepositoryApply(repository, getRepositoryField());
    }

    @Override
    public String getMessage() {
        return commonRepositoryTaskHelper.getTaskMessage(MetaDataTaskDescriptor.BLACK_DUCK_META_DATA_TASK_NAME, getRepositoryField());
    }

}
