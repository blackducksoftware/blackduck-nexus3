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
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.nexus3.BlackDuckConnection;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanel;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.phonehome.BlackDuckPhoneHomeHelper;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.phonehome.PhoneHomeResponse;

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

    // TODO move this to the filter class
    public boolean doesRepositoryApply(final Repository repository, final String repositoryField) {
        return repository.getName().equals(repositoryField);
    }

    public String getTaskMessage(final String taskName, final String repositoryField) {
        return String.format("Running %s for repository %s: ", taskName, repositoryField);
    }

    public QueryManager getQueryManager() {
        return queryManager;
    }

    public BlackDuckServerConfig getBlackDuckServerConfig() throws IntegrationException, IllegalStateException {
        return blackDuckConnection.getBlackDuckServerConfig();
    }

    public BlackDuckServicesFactory getBlackDuckServicesFactory() throws IntegrationException, IllegalStateException {
        return blackDuckConnection.getBlackDuckServicesFactory();
    }

    public void failedConnection(final AssetWrapper assetWrapper, final String exceptionMessage) {
        assetWrapper.removeAllBlackDuckData();
        assetWrapper.addFailureToBlackDuckPanel("Error connecting to Black Duck. " + exceptionMessage);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
    }

    public void failedConnection(final AssetWrapper assetWrapper) {
        logger.error("Failed to connect to Black Duck");
        assetWrapper.removeAllBlackDuckData();
        assetWrapper.addFailureToBlackDuckPanel("Error connecting to Black Duck.");
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
    }

    public void closeConnection() {
        blackDuckConnection.closeBlackDuckRestConnection();
    }

    public void phoneHome(final String taskName) {
        final PhoneHome phoneHome = new PhoneHome(blackDuckConnection);
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            final BlackDuckPhoneHomeHelper blackDuckPhoneHomeHelper = phoneHome.createBlackDuckPhoneHomeHelper(executorService);
            logger.debug("Sending phone home data.");
            final PhoneHomeResponse response = phoneHome.sendDataHome(taskName, blackDuckPhoneHomeHelper);
            response.awaitResult();
        } catch (final IntegrationException e) {
            logger.debug("There was an error communicating with BlackDuck while phoning home: {}", e.getMessage());
        } finally {
            executorService.shutdownNow();
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
        if (StringUtils.isBlank(directoryName)) {
            return new File(CommonDescriptorHelper.DEFAULT_WORKING_DIRECTORY);
        }
        return new File(directoryName);
    }

    public String getBlackDuckPanelPath(final AssetPanelLabel assetPanelLabel) {
        final String dbXmlPath = "attributes." + AssetPanel.BLACKDUCK_CATEGORY + ".";
        return dbXmlPath + assetPanelLabel.getLabel();
    }

    public Optional<ProjectVersionWrapper> getProjectVersionWrapper(final BlackDuckServicesFactory blackDuckServicesFactory, final String name, final String version) throws IntegrationException {
        final ProjectService projectService = blackDuckServicesFactory.createProjectService();
        return projectService.getProjectVersion(name, version);
    }

    //    public String verifyUpload(final BlackDuckServicesFactory blackDuckServicesFactory, final String codeLocationName, final String name, final String version) {
    //        try {
    //            final ProjectVersionWrapper projectVersionWrapper = getProjectVersionWrapper(blackDuckServicesFactory, name, version);
    //            return verifyUpload(blackDuckServicesFactory, codeLocationName, projectVersionWrapper.getProjectVersionView());
    //        } catch (final IntegrationException e) {
    //            logger.error("Problem communicating with BlackDuck: {}", e.getMessage());
    //            return VERIFICATION_ERROR + e.getMessage();
    //        }
    //    }
    //
    //    public String verifyUpload(final BlackDuckServicesFactory blackDuckServicesFactory, final String codeLocationName, final ProjectVersionView projectVersionView) {
    //        logger.debug("Checking that project exists in BlackDuck.");
    //        try {
    //            final CodeLocationService codeLocationService = blackDuckServicesFactory.createCodeLocationService();
    //            final BlackDuckService blackDuckService = blackDuckServicesFactory.createBlackDuckService();
    //
    //            final Optional<CodeLocationView> codeLocationViewOptional = codeLocationService.getCodeLocationByName(codeLocationName);
    //            final List<ScanSummaryView> scanSummaryViews = new ArrayList<>();
    //            if (codeLocationViewOptional.isPresent()) {
    //                final CodeLocationView codeLocationView = codeLocationViewOptional.get();
    //                codeLocationView.getFirstLink(CodeLocationView.SCANS_LINK);
    //                final Optional<String> scansLinkOptional = codeLocationView.getFirstLink(CodeLocationView.SCANS_LINK);
    //                if (scansLinkOptional.isPresent()) {
    //                    final List<ScanSummaryView> codeLocationScanSummaryViews = blackDuckService.getResponses(scansLinkOptional.get(), ScanSummaryView.class, true);
    //                    scanSummaryViews.addAll(codeLocationScanSummaryViews);
    //                }
    //
    //                final CodeLocationService scanStatusService = blackDuckServicesFactory.createCodeLocationService(ScanStatusService.DEFAULT_TIMEOUT * 4);
    //                scanStatusService.assertScansFinished(scanSummaryViews);
    //
    //                return blackDuckService.getHref(projectVersionView);
    //            }
    //        } catch (final IntegrationException e) {
    //            logger.error("Problem communicating with BlackDuck: {}", e.getMessage());
    //            return VERIFICATION_ERROR + e.getMessage();
    //        } catch (final InterruptedException e) {
    //            throw new TaskInterruptedException("Waiting for the scans to finish was interrupted: " + e.getMessage(), true);
    //        }
    //    }

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
