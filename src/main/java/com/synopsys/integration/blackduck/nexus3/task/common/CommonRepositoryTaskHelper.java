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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import com.synopsys.integration.blackduck.api.generated.view.CodeLocationView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.view.ScanSummaryView;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.BlackDuckConnection;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanel;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.CodeLocationService;
import com.synopsys.integration.blackduck.service.HubService;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.ScanStatusService;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.phonehome.PhoneHomeCallable;

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

    public HubServerConfig getHubServerConfig() throws IntegrationException, IllegalStateException {
        return blackDuckConnection.getHubServerConfig();
    }

    public HubServicesFactory getHubServicesFactory() throws IntegrationException, IllegalStateException {
        return blackDuckConnection.getHubServicesFactory();
    }

    public void failedConnection(final AssetWrapper assetWrapper, final String exceptionMessage) {
        logger.error("Failed to connect to Black Duck. Asset: {} {}", assetWrapper.getName(), assetWrapper.getVersion());
        assetWrapper.removeAllBlackDuckData();
        assetWrapper.addFailureToBlackDuckPanel("Error connecting to Black Duck. " + exceptionMessage);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
    }

    public void closeConnection() {
        try {
            blackDuckConnection.closeBlackDuckRestConnection();
        } catch (final IOException e) {
            logger.error("Issue trying to close the connection to BlackDuck: {}", e.getMessage());
        }
    }

    public void phoneHome(final String taskName) {
        final PhoneHome phoneHome = new PhoneHome(blackDuckConnection);
        try {
            final PhoneHomeCallable phoneHomeCallable = phoneHome.createPhoneHomeCallable(taskName);
            logger.debug("Sending phone home data.");
            phoneHome.sendDataHome(phoneHomeCallable);
        } catch (final IntegrationException e) {
            logger.debug("There was an error communicating with BlackDuck while phoning home: {}", e.getMessage());
        }

    }

    public String getRepositoryPath(final TaskConfiguration taskConfiguration) {
        return taskConfiguration.getString(CommonTaskKeys.REPOSITORY_PATH.getParameterKey());
    }

    public String getFileExtensionPatterns(final TaskConfiguration taskConfiguration) {
        return taskConfiguration.getString(CommonTaskKeys.FILE_PATTERNS.getParameterKey());
    }

    public DateTime getAssetCutoffDateTime(final TaskConfiguration taskConfiguration) {
        final String assetCutoffString = taskConfiguration.getString(CommonTaskKeys.OLD_ASSET_CUTOFF.getParameterKey());
        return dateTimeParser.convertFromStringToDate(assetCutoffString);
    }

    public File getWorkingDirectory(final TaskConfiguration taskConfiguration) {
        final String directoryName = taskConfiguration.getString(CommonTaskKeys.WORKING_DIRECTORY.getParameterKey());
        return new File(directoryName);
    }

    public String getBlackDuckPanelPath(final AssetPanelLabel assetPanelLabel) {
        final String dbXmlPath = "attributes." + AssetPanel.BLACKDUCK_CATEGORY + ".";
        return dbXmlPath + assetPanelLabel.getLabel();
    }

    public ProjectVersionWrapper getProjectVersionWrapper(final HubServicesFactory hubServicesFactory, final String name, final String version) throws IntegrationException {
        final ProjectService projectService = hubServicesFactory.createProjectService();
        return projectService.getProjectVersion(name, version);
    }

    public String verifyUpload(final HubServicesFactory hubServicesFactory, final String codeLocationName, final String name, final String version) {
        try {
            final ProjectVersionWrapper projectVersionWrapper = getProjectVersionWrapper(hubServicesFactory, name, version);
            return verifyUpload(hubServicesFactory, codeLocationName, projectVersionWrapper.getProjectVersionView());
        } catch (final IntegrationException e) {
            logger.error("Problem communicating with BlackDuck: {}", e.getMessage());
            return VERIFICATION_ERROR + e.getMessage();
        }
    }

    public String verifyUpload(final HubServicesFactory hubServicesFactory, final String codeLocationName, final ProjectVersionView projectVersionView) {
        logger.debug("Checking that project exists in BlackDuck.");
        try {
            final CodeLocationService codeLocationService = hubServicesFactory.createCodeLocationService();
            final HubService hubService = hubServicesFactory.createHubService();

            final CodeLocationView codeLocationView = codeLocationService.getCodeLocationByName(codeLocationName);
            final List<ScanSummaryView> scanSummaryViews = new ArrayList<>();
            final String scansLink = hubService.getFirstLinkSafely(codeLocationView, CodeLocationView.SCANS_LINK);
            if (StringUtils.isNotBlank(scansLink)) {
                final List<ScanSummaryView> codeLocationScanSummaryViews = hubService.getResponses(scansLink, ScanSummaryView.class, true);
                scanSummaryViews.addAll(codeLocationScanSummaryViews);
            }

            final ScanStatusService scanStatusService = hubServicesFactory.createScanStatusService(ScanStatusService.DEFAULT_TIMEOUT * 4);
            scanStatusService.assertScansFinished(scanSummaryViews);

            return hubService.getHref(projectVersionView);
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

}
