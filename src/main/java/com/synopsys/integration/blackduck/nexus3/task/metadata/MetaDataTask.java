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

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;

import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationService;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonMetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonTaskFilters;
import com.synopsys.integration.blackduck.nexus3.task.inspector.InspectorMetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.task.scan.ScanMetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectBomService;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.phonehome.PhoneHomeResponse;

@Named
public class MetaDataTask extends RepositoryTaskSupport {
    private final Logger logger = LoggerFactory.getLogger(getClass());
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
        Optional<PhoneHomeResponse> phoneHomeResponse = commonRepositoryTaskHelper.phoneHome(MetaDataTaskDescriptor.BLACK_DUCK_META_DATA_TASK_ID);

        String exceptionMessage = null;
        CodeLocationCreationService codeLocationCreationService = null;
        BlackDuckService blackDuckService = null;
        ProjectService projectService = null;
        ProjectBomService projectBomService = null;
        try {
            BlackDuckServicesFactory blackDuckServicesFactory = commonRepositoryTaskHelper.getBlackDuckServicesFactory();
            codeLocationCreationService = blackDuckServicesFactory.createCodeLocationCreationService();
            blackDuckService = blackDuckServicesFactory.createBlackDuckService();
            projectService = blackDuckServicesFactory.createProjectService();
            projectBomService = blackDuckServicesFactory.createProjectBomService();
        } catch (IntegrationException | IllegalStateException e) {
            logger.error(String.format("Black Duck hub server config invalid. %s", e.getMessage()), e);
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

            MetaDataScanConfiguration metaDataScanConfiguration;
            if (StringUtils.isNotBlank(exceptionMessage)) {
                metaDataScanConfiguration = MetaDataScanConfiguration.createConfigurationWithError(exceptionMessage, repository, isProxyRepo, assetStatusLabel);
            } else {
                metaDataScanConfiguration = MetaDataScanConfiguration.createConfiguration(repository, isProxyRepo, assetStatusLabel, codeLocationCreationService, blackDuckService, projectService, projectBomService);
            }
            MetadataRepositoryScanner metadataRepositoryScanner = new MetadataRepositoryScanner(commonRepositoryTaskHelper, queryManager, commonMetaDataProcessor, inspectorMetaDataProcessor,
                scanMetaDataProcessor, dateTimeParser, metaDataScanConfiguration);
            metadataRepositoryScanner.scanRepository();
        }
        if (phoneHomeResponse.isPresent()) {
            commonRepositoryTaskHelper.endPhoneHome(phoneHomeResponse.get());
        } else {
            logger.debug("Could not phone home.");
        }
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
