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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.NotificationTaskRange;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.phonehome.PhoneHomeResponse;

@Named
public class MetaDataTask extends RepositoryTaskSupport {
    private final Logger logger = createLogger();
    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final QueryManager queryManager;
    private final CommonMetaDataProcessor commonMetaDataProcessor;
    private final InspectorMetaDataProcessor inspectorMetaDataProcessor;
    private final ScanMetaDataProcessor scanMetaDataProcessor;
    private final DateTimeParser dateTimeParser;
    private final CommonTaskFilters commonTaskFilters;

    @Inject
    public MetaDataTask(final CommonRepositoryTaskHelper commonRepositoryTaskHelper, final QueryManager queryManager, final CommonMetaDataProcessor commonMetaDataProcessor, final InspectorMetaDataProcessor inspectorMetaDataProcessor,
        final ScanMetaDataProcessor scanMetaDataProcessor, final DateTimeParser dateTimeParser, final CommonTaskFilters commonTaskFilters) {
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.queryManager = queryManager;
        this.commonMetaDataProcessor = commonMetaDataProcessor;
        this.inspectorMetaDataProcessor = inspectorMetaDataProcessor;
        this.scanMetaDataProcessor = scanMetaDataProcessor;
        this.dateTimeParser = dateTimeParser;
        this.commonTaskFilters = commonTaskFilters;
    }

    @Override
    protected void execute(final Repository repository) {
        String exceptionMessage = null;
        String blackDuckUrl = null;
        CodeLocationCreationService codeLocationCreationService = null;
        NotificationTaskRange notificationTaskRange = null;
        ProjectService projectService = null;
        Optional<PhoneHomeResponse> phoneHomeResponse = Optional.empty();
        try {
            blackDuckUrl = commonRepositoryTaskHelper.getBlackDuckServerConfig().getBlackDuckUrl().toString();
            final BlackDuckServicesFactory blackDuckServicesFactory = commonRepositoryTaskHelper.getBlackDuckServicesFactory();
            codeLocationCreationService = blackDuckServicesFactory.createCodeLocationCreationService();
            projectService = blackDuckServicesFactory.createProjectService();
            phoneHomeResponse = commonRepositoryTaskHelper.phoneHome(MetaDataTaskDescriptor.BLACK_DUCK_META_DATA_TASK_ID);
        } catch (final IntegrationException | IllegalStateException e) {
            logger.error("Black Duck hub server config invalid. " + e.getMessage(), e);
            exceptionMessage = e.getMessage();
        }
        for (final Repository foundRepository : commonTaskFilters.findRelevantRepositories(repository)) {
            final String repoName = foundRepository.getName();
            final boolean isProxyRepo = commonTaskFilters.isProxyRepository(foundRepository.getType());
            logger.info("Checking repository for assets: {}", repoName);

            final AssetPanelLabel assetStatusLabel;
            if (isProxyRepo) {
                assetStatusLabel = AssetPanelLabel.INSPECTION_TASK_STATUS;
            } else {
                assetStatusLabel = AssetPanelLabel.SCAN_TASK_STATUS;
            }

            final Query filteredAssets = createFilteredQuery(assetStatusLabel, Optional.empty());
            PagedResult<Asset> pagedAssets = commonRepositoryTaskHelper.retrievePagedAssets(foundRepository, filteredAssets);
            final Map<String, AssetWrapper> assetWrapperMap = new HashMap<>();

            while (pagedAssets.hasResults()) {
                logger.debug("Found items in the DB.");
                for (final Asset asset : pagedAssets.getTypeList()) {
                    final AssetWrapper assetWrapper = AssetWrapper.createAssetWrapper(asset, foundRepository, queryManager, assetStatusLabel);
                    if (StringUtils.isNotBlank(exceptionMessage)) {
                        commonRepositoryTaskHelper.failedConnection(assetWrapper, exceptionMessage);
                        assetWrapper.updateAsset();
                    } else {
                        final String name = assetWrapper.getName();
                        logger.info("Updating metadata for {}", name);
                        try {
                            if (!isProxyRepo) {
                                final String assetBlackDuckUrl = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.BLACKDUCK_URL);
                                final TaskStatus status = assetWrapper.getBlackDuckStatus();
                                final String lastProcessedString = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME);
                                final DateTime lastProcessed = dateTimeParser.convertFromStringToDate(lastProcessedString);
                                final String version = assetWrapper.getVersion();
                                Optional<ProjectVersionWrapper> projectVersionWrapper = Optional.empty();

                                if (StringUtils.isBlank(assetBlackDuckUrl) && isPendingOrComponentNotFoundForDay(status, lastProcessed)) {
                                    final String codeLocationName = scanMetaDataProcessor.createCodeLocationName(repoName, name, version);
                                    logger.info("Re-checking code location {}", codeLocationName);
                                    if (null != codeLocationCreationService && null != projectService) {
                                        if (null == notificationTaskRange) {
                                            notificationTaskRange = codeLocationCreationService.calculateCodeLocationRange();
                                        }
                                        codeLocationCreationService
                                            .waitForCodeLocations(notificationTaskRange, Collections.singleton(codeLocationName), commonRepositoryTaskHelper.getBlackDuckServerConfig().getTimeout() * 5);
                                        projectVersionWrapper = projectService.getProjectVersion(name, version);
                                    }
                                } else {
                                    if (null != projectService) {
                                        projectVersionWrapper = projectService.getProjectVersion(name, version);
                                    }
                                }
                                if (projectVersionWrapper.isPresent()) {
                                    final ProjectVersionView projectVersionView = projectVersionWrapper.get().getProjectVersionView();
                                    logger.info("Updating data of hosted repository.");
                                    scanMetaDataProcessor.updateRepositoryMetaData(projectService, assetWrapper, projectVersionView.getHref().orElse(assetBlackDuckUrl), projectVersionView);

                                } else {
                                    updateAssetWrapperWithError(assetWrapper, String.format("Could not find project %s and version %s after the scan completed.", name, version));
                                }
                            } else {
                                final String originId = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.ASSET_ORIGIN_ID);
                                assetWrapperMap.put(originId, assetWrapper);
                            }
                        } catch (final IntegrationException e) {
                            updateAssetWrapperWithError(assetWrapper, e.getMessage());
                            throw new TaskInterruptedException("Problem checking metadata: " + e.getMessage(), true);
                        } catch (final InterruptedException e) {
                            updateAssetWrapperWithError(assetWrapper, "Waiting for the scan to complete was interrupted: " + e.getMessage());
                            logger.error("Problem communicating with Black Duck: {}", e.getMessage());
                        }
                    }
                }

                final Query nextPage = createFilteredQuery(assetStatusLabel, pagedAssets.getLastName());
                pagedAssets = commonRepositoryTaskHelper.retrievePagedAssets(foundRepository, nextPage);
            }

            if (isProxyRepo && null != projectService) {
                logger.info("Updating data of proxy repository.");
                try {
                    final ProjectVersionView projectVersionView = inspectorMetaDataProcessor.getOrCreateProjectVersion(projectService, repoName);
                    inspectorMetaDataProcessor.updateRepositoryMetaData(projectService, blackDuckUrl, projectVersionView, assetWrapperMap, TaskStatus.SUCCESS);
                } catch (final IntegrationException e) {
                    for (final Map.Entry<String, AssetWrapper> entry : assetWrapperMap.entrySet()) {
                        updateAssetWrapperWithError(entry.getValue(), String.format("Problem retrieving the project %s from Hub: %s", repoName, e.getMessage()));
                        logger.error("Problem communicating with Black Duck: {}", e.getMessage());
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

    private void updateAssetWrapperWithError(final AssetWrapper assetWrapper, final String message) {
        commonMetaDataProcessor.removeAllMetaData(assetWrapper);
        assetWrapper.addFailureToBlackDuckPanel(message);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
        assetWrapper.updateAsset();
    }

    private Query createFilteredQuery(final AssetPanelLabel statusLabel, final Optional<String> lastNameUsed) {
        final Query.Builder pagedQueryBuilder = commonRepositoryTaskHelper.createPagedQuery(lastNameUsed);
        final String statusPath = commonRepositoryTaskHelper.getBlackDuckPanelPath(statusLabel);
        final String oldStatusPath = commonRepositoryTaskHelper.getBlackDuckPanelPath(AssetPanelLabel.OLD_STATUS);
        pagedQueryBuilder.and(statusWhereStatement(statusPath)).or(statusWhereStatement(oldStatusPath));
        return pagedQueryBuilder.build();
    }

    private String statusWhereStatement(final String blackDuckDbPath) {
        final StringBuilder statusWhere = new StringBuilder();

        statusWhere.append("(");
        statusWhere.append(createEqualsStatement(blackDuckDbPath, TaskStatus.SUCCESS.name()));
        statusWhere.append(" OR ");
        statusWhere.append(createEqualsStatement(blackDuckDbPath, TaskStatus.PENDING.name()));
        statusWhere.append(" OR ");
        statusWhere.append(createEqualsStatement(blackDuckDbPath, TaskStatus.COMPONENT_NOT_FOUND.name()));
        statusWhere.append(")");

        return statusWhere.toString();
    }

    private String createEqualsStatement(final String object, final String value) {
        final StringBuilder equalsStatement = new StringBuilder();
        equalsStatement.append(object);
        equalsStatement.append(" = '");
        equalsStatement.append(value);
        equalsStatement.append("'");
        return equalsStatement.toString();
    }

    private boolean isPendingOrComponentNotFoundForDay(final TaskStatus status, final DateTime lastProcessed) {
        if (TaskStatus.PENDING.equals(status) || TaskStatus.COMPONENT_NOT_FOUND.equals(status)) {
            final String timeNow = dateTimeParser.getCurrentDateTime();
            final DateTime now = dateTimeParser.convertFromStringToDate(timeNow);
            return now.isAfter(lastProcessed.plusDays(1));
        }

        return false;
    }

    @Override
    protected boolean appliesTo(final Repository repository) {
        return commonRepositoryTaskHelper.doesRepositoryApply(repository, getRepositoryField());
    }

    @Override
    public String getMessage() {
        return commonRepositoryTaskHelper.getTaskMessage(MetaDataTaskDescriptor.BLACK_DUCK_META_DATA_TASK_NAME, getRepositoryField());
    }

}
