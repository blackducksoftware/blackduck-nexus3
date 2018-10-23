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
package com.synopsys.integration.blackduck.nexus3.task.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.api.generated.view.CodeLocationView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.view.ScanSummaryView;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.BlackDuckConnection;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanel;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.CodeLocationService;
import com.synopsys.integration.blackduck.service.HubService;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.ScanStatusService;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;

@Named
@Singleton
public class CommonRepositoryTaskHelper {
    public static final int DEFAULT_PAGE_SIZE = 100;
    public static final String VERIFICATION_ERROR = "Error retrieving URL: ";
    private final QueryManager queryManager;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DateTimeParser dateTimeParser;
    private final BlackDuckConnection blackDuckConnection;

    @Inject
    public CommonRepositoryTaskHelper(final QueryManager queryManager, final DateTimeParser dateTimeParser, final BlackDuckConnection blackDuckConnection) {
        this.queryManager = queryManager;
        this.dateTimeParser = dateTimeParser;
        this.blackDuckConnection = blackDuckConnection;
    }

    public boolean doesRepositoryApply(final Repository repository, final String repositoryField) {
        return repository.getName().equals(repositoryField);
    }

    public String getTaskMessage(final String taskName, final String repositoryField) {
        return String.format("Running %s for repository %s: ", taskName, repositoryField);
    }

    public QueryManager getQueryManager() {
        return queryManager;
    }

    public HubServerConfig getHubServerConfig() {
        try {
            return blackDuckConnection.getHubServerConfig();
        } catch (final IntegrationException e) {
            throw new TaskInterruptedException("BlackDuck hub server config not set.", true);
        }
    }

    public HubServicesFactory getHubServicesFactory() {
        try {
            return blackDuckConnection.getHubServicesFactory();
        } catch (final IntegrationException e) {
            throw new TaskInterruptedException("BlackDuck hub server config not set.", true);
        }
    }

    public String getRepositoryPath(final TaskConfiguration taskConfiguration) {
        return taskConfiguration.getString(CommonTaskKeys.REPOSITORY_PATH.getParameterKey());
    }

    public String getFileExtensionPatterns(final TaskConfiguration taskConfiguration) {
        return taskConfiguration.getString(CommonTaskKeys.FILE_PATTERNS.getParameterKey());
    }

    public DateTime getAssetCutoffDateTime(final TaskConfiguration taskConfiguration) {
        final String artifactCutoffString = taskConfiguration.getString(CommonTaskKeys.OLD_ARTIFACT_CUTOFF.getParameterKey());
        return dateTimeParser.convertFromStringToDate(artifactCutoffString);
    }

    public File getWorkingDirectory(final TaskConfiguration taskConfiguration) {
        final String directoryName = taskConfiguration.getString(CommonTaskKeys.WORKING_DIRECTORY.getParameterKey());
        return new File(directoryName);
    }

    public boolean skipAssetProcessing(final AssetWrapper assetWrapper, final TaskConfiguration taskConfiguration) {
        final DateTime lastModified = assetWrapper.getComponentLastUpdated();
        logger.debug("Last modified: {}", lastModified);
        final String fullPathName = assetWrapper.getAsset().name();
        logger.debug("Asset full path name: {}", fullPathName);
        final boolean doesRepositoryPathMatch = doesRepositoryPathMatch(fullPathName, getRepositoryPath(taskConfiguration));
        final boolean isAssetTooOld = isAssetTooOld(getAssetCutoffDateTime(taskConfiguration), lastModified);
        final boolean doesExtensionMatch = doesExtensionMatch(assetWrapper.getFilename(), getFileExtensionPatterns(taskConfiguration));
        final String lastProcessedString = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME);
        logger.debug("Last processed: {}", lastProcessedString);
        final DateTime lastProcessed = dateTimeParser.convertFromStringToDate(lastProcessedString);
        final boolean processAgain = lastProcessed != null && lastModified.isAfter(lastProcessed);
        return isAssetTooOld || !doesRepositoryPathMatch || !doesExtensionMatch || processAgain;
    }

    public boolean doesExtensionMatch(final String filename, final String allowedExtensions) {
        final Set<String> extensions = Arrays.stream(allowedExtensions.split(",")).map(String::trim).collect(Collectors.toSet());
        for (final String extensionPattern : extensions) {
            if (FilenameUtils.wildcardMatch(filename, extensionPattern)) {
                return true;
            }
        }
        return false;
    }

    public boolean doesRepositoryPathMatch(final String assetPath, final String regexPattern) {
        if (StringUtils.isBlank(regexPattern)) {
            logger.debug("Repository path is blank.");
            return true;
        }
        logger.debug("Artifact Path {} being checked against {}", assetPath, regexPattern);
        return Pattern.matches(regexPattern, assetPath);
    }

    public boolean isAssetTooOld(final DateTime cutoffDate, final DateTime lastUpdated) {
        return lastUpdated.isBefore(cutoffDate.getMillis());
    }

    public String getBlackDuckPanelPath(final AssetPanelLabel assetPanelLabel) {
        final String dbXmlPath = "attributes." + AssetPanel.BLACKDUCK_CATEGORY + ".";
        return dbXmlPath + assetPanelLabel.getLabel();
    }

    public ProjectVersionWrapper getProjectVersionWrapper(final String name, final String version) throws IntegrationException {
        final ProjectService projectService = getHubServicesFactory().createProjectService();
        return projectService.getProjectVersion(name, version);
    }

    public String verifyUpload(final List<String> codeLocationNames, final String name, final String version) {
        try {
            final ProjectVersionWrapper projectVersionWrapper = getProjectVersionWrapper(name, version);
            return verifyUpload(codeLocationNames, projectVersionWrapper.getProjectVersionView());
        } catch (final IntegrationException e) {
            logger.error("Problem communicating with BlackDuck: {}", e.getMessage());
            return VERIFICATION_ERROR + e.getMessage();
        }
    }

    public String verifyUpload(final List<String> codeLocationNames, final ProjectVersionView projectVersionView) {
        logger.debug("Checking that project exists in BlackDuck.");
        try {
            final CodeLocationService codeLocationService = getHubServicesFactory().createCodeLocationService();
            final HubService hubService = getHubServicesFactory().createHubService();

            final List<CodeLocationView> allCodeLocations = new ArrayList<>();
            for (final String codeLocationName : codeLocationNames) {
                final CodeLocationView codeLocationView = codeLocationService.getCodeLocationByName(codeLocationName);
                allCodeLocations.add(codeLocationView);
            }
            final List<ScanSummaryView> scanSummaryViews = new ArrayList<>();
            for (final CodeLocationView codeLocationView : allCodeLocations) {
                final String scansLink = hubService.getFirstLinkSafely(codeLocationView, CodeLocationView.SCANS_LINK);
                if (StringUtils.isNotBlank(scansLink)) {
                    final List<ScanSummaryView> codeLocationScanSummaryViews = hubService.getResponses(scansLink, ScanSummaryView.class, true);
                    scanSummaryViews.addAll(codeLocationScanSummaryViews);
                }
            }

            final ScanStatusService scanStatusService = getHubServicesFactory().createScanStatusService(ScanStatusService.DEFAULT_TIMEOUT);
            scanStatusService.assertScansFinished(scanSummaryViews);

            return hubService.getFirstLink(projectVersionView, ProjectVersionView.COMPONENTS_LINK);
        } catch (final IntegrationException | InterruptedException e) {
            logger.error("Problem communicating with BlackDuck: {}", e.getMessage());
            return VERIFICATION_ERROR + e.getMessage();
        }
    }

    public Query.Builder createPagedQuery(final Optional<String> lastNameUsed) {
        final Query.Builder pagedQueryBuilder = Query.builder();
        pagedQueryBuilder.where("component").isNotNull();
        if (lastNameUsed.isPresent()) {
            pagedQueryBuilder.and("name > ").param(lastNameUsed.get());
        }

        pagedQueryBuilder.suffix(String.format("ORDER BY name LIMIT %d", DEFAULT_PAGE_SIZE));
        return pagedQueryBuilder;
    }

    public PagedResult<Asset> retrievePagedAssets(final Repository repository, final Query filteredQuery) {
        logger.debug("Running where statement from asset table of: {}. With the parameters: {}. And suffix: {}", filteredQuery.getWhere(), filteredQuery.getParameters(), filteredQuery.getQuerySuffix());
        final Iterable<Asset> filteredAssets = queryManager.findAssetsInRepository(repository, filteredQuery);
        final Optional<Asset> lastReturnedAsset = StreamSupport.stream(filteredAssets.spliterator(), true).reduce((first, second) -> second);
        Optional<String> name = Optional.empty();
        if (lastReturnedAsset.isPresent()) {
            name = Optional.of(lastReturnedAsset.get().name());
        }
        return new PagedResult<>(filteredAssets, name);
    }

    // TODO Pull most query building code out of here and make query building easier
    public Query createFilteredQueryBuilder(final boolean rescanFailures, final boolean alwaysScan, final Optional<String> lastNameUsed) {
        final Query.Builder baseQueryBuilder = createPagedQuery(lastNameUsed);

        final String statusSuccess = createSuccessWhereStatement(rescanFailures, alwaysScan);
        baseQueryBuilder.and(statusSuccess);

        return baseQueryBuilder.build();
    }

    public String createSuccessWhereStatement(final boolean checkFailures, final boolean checkSuccessAndPending) {
        final String statusPath = getBlackDuckPanelPath(AssetPanelLabel.TASK_STATUS);
        final StringBuilder extensionsWhereBuilder = new StringBuilder();
        extensionsWhereBuilder.append("(");
        extensionsWhereBuilder.append(statusPath + " IS NULL");
        if (checkSuccessAndPending) {
            extensionsWhereBuilder.append(" OR ");
            extensionsWhereBuilder.append(statusPath);
            extensionsWhereBuilder.append(" = '");
            extensionsWhereBuilder.append(TaskStatus.SUCCESS.name());
            extensionsWhereBuilder.append("'");
            extensionsWhereBuilder.append(" OR ");
            extensionsWhereBuilder.append(statusPath);
            extensionsWhereBuilder.append(" = '");
            extensionsWhereBuilder.append(TaskStatus.PENDING.name());
            extensionsWhereBuilder.append("'");
        }
        if (checkFailures) {
            extensionsWhereBuilder.append(" OR ");
            extensionsWhereBuilder.append(statusPath);
            extensionsWhereBuilder.append(" = '");
            extensionsWhereBuilder.append(TaskStatus.FAILURE.name());
            extensionsWhereBuilder.append("'");
        }

        extensionsWhereBuilder.append(")");
        return extensionsWhereBuilder.toString();
    }

}