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
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.phonehome.PhoneHomeResponse;

@Named
@Singleton
public class CommonRepositoryTaskHelper {
    public static final int DEFAULT_PAGE_SIZE = 100;
    private final QueryManager queryManager;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DateTimeParser dateTimeParser;
    private final BlackDuckConnection blackDuckConnection;

    @Inject
    public CommonRepositoryTaskHelper(QueryManager queryManager, DateTimeParser dateTimeParser, BlackDuckConnection blackDuckConnection) {
        this.queryManager = queryManager;
        this.dateTimeParser = dateTimeParser;
        this.blackDuckConnection = blackDuckConnection;
    }

    public String getTaskMessage(String taskName, String repositoryField) {
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

    public void failedConnection(AssetWrapper assetWrapper, String exceptionMessage) {
        assetWrapper.removeAllBlackDuckData();
        assetWrapper.addFailureToBlackDuckPanel("Error connecting to Black Duck. " + exceptionMessage);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
    }

    public Optional<PhoneHomeResponse> phoneHome(String taskName) {
        PhoneHome phoneHome = new PhoneHome(blackDuckConnection);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            BlackDuckPhoneHomeHelper blackDuckPhoneHomeHelper = phoneHome.createBlackDuckPhoneHomeHelper(executorService);
            logger.debug("Sending phone home data.");
            PhoneHomeResponse response = phoneHome.sendDataHome(taskName, blackDuckPhoneHomeHelper);
            return Optional.of(response);
        } catch (Exception e) {
            logger.debug("There was an error communicating with Black Duck while phoning home.");
            logger.debug(e.getMessage(), e);
        } finally {
            executorService.shutdownNow();
        }
        return Optional.empty();
    }

    public void endPhoneHome(PhoneHomeResponse phoneHomeResponse) {
        if (phoneHomeResponse.getImmediateResult()) {
            logger.debug("Phone home was successful.");
        } else {
            logger.debug("Phone home failed.");
        }
    }

    public File getWorkingDirectory(TaskConfiguration taskConfiguration) {
        String directoryName = taskConfiguration.getString(CommonTaskKeys.WORKING_DIRECTORY.getParameterKey());
        if (StringUtils.isBlank(directoryName)) {
            return new File(CommonDescriptorHelper.DEFAULT_WORKING_DIRECTORY);
        }
        return new File(directoryName);
    }

    public String getBlackDuckPanelPath(AssetPanelLabel assetPanelLabel) {
        final String dbXmlPath = "attributes." + AssetPanel.BLACKDUCK_CATEGORY + ".";
        return dbXmlPath + assetPanelLabel.getLabel();
    }

    public Query.Builder createPagedQuery(Optional<String> lastNameUsed) {
        Query.Builder pagedQueryBuilder = Query.builder();
        pagedQueryBuilder.where("component").isNotNull();
        if (lastNameUsed.isPresent()) {
            pagedQueryBuilder.and("name > ").param(lastNameUsed.get());
        }

        pagedQueryBuilder.suffix(String.format("ORDER BY name LIMIT %d", DEFAULT_PAGE_SIZE));
        return pagedQueryBuilder;
    }

    public PagedResult<Asset> retrievePagedAssets(Repository repository, Query filteredQuery) {
        logger.debug("Running where statement from asset table of: {}. With the parameters: {}. And suffix: {}", filteredQuery.getWhere(), filteredQuery.getParameters(), filteredQuery.getQuerySuffix());
        Iterable<Asset> filteredAssets = queryManager.findAssetsInRepository(repository, filteredQuery);
        Optional<Asset> lastReturnedAsset = StreamSupport.stream(filteredAssets.spliterator(), true).reduce((first, second) -> second);
        Optional<String> name = Optional.empty();
        if (lastReturnedAsset.isPresent()) {
            name = Optional.of(lastReturnedAsset.get().name());
        }
        return new PagedResult<>(filteredAssets, name);
    }

}
