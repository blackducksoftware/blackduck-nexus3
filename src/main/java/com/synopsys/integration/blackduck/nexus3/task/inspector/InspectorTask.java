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
package com.synopsys.integration.blackduck.nexus3.task.inspector;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationService;
import com.synopsys.integration.blackduck.codelocation.bdioupload.BdioUploadService;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonTaskFilters;
import com.synopsys.integration.blackduck.nexus3.task.inspector.dependency.DependencyGenerator;
import com.synopsys.integration.blackduck.nexus3.task.inspector.dependency.DependencyType;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.ComponentService;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.phonehome.PhoneHomeResponse;

@Named
public class InspectorTask extends RepositoryTaskSupport {
    public static final String INSPECTOR_VERSION_NAME = "Nexus3Inspection";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final DateTimeParser dateTimeParser;
    private final DependencyGenerator dependencyGenerator;
    private final InspectorMetaDataProcessor inspectorMetaDataProcessor;
    private final CommonTaskFilters commonTaskFilters;

    @Inject
    public InspectorTask(CommonRepositoryTaskHelper commonRepositoryTaskHelper, DateTimeParser dateTimeParser, DependencyGenerator dependencyGenerator, InspectorMetaDataProcessor inspectorMetaDataProcessor,
        CommonTaskFilters commonTaskFilters) {
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.dateTimeParser = dateTimeParser;
        this.dependencyGenerator = dependencyGenerator;
        this.inspectorMetaDataProcessor = inspectorMetaDataProcessor;
        this.commonTaskFilters = commonTaskFilters;
    }

    @Override
    protected void execute(Repository repository) {
        String exceptionMessage = null;
        BlackDuckService blackDuckService = null;
        ProjectService projectService = null;
        CodeLocationCreationService codeLocationCreationService = null;
        BdioUploadService bdioUploadService = null;
        ComponentService componentService = null;
        Optional<PhoneHomeResponse> phoneHomeResponse = Optional.empty();
        try {
            BlackDuckServicesFactory blackDuckServicesFactory = commonRepositoryTaskHelper.getBlackDuckServicesFactory();
            blackDuckService = blackDuckServicesFactory.createBlackDuckService();
            projectService = blackDuckServicesFactory.createProjectService();
            codeLocationCreationService = blackDuckServicesFactory.createCodeLocationCreationService();
            bdioUploadService = blackDuckServicesFactory.createBdioUploadService();
            componentService = blackDuckServicesFactory.createComponentService();
            phoneHomeResponse = commonRepositoryTaskHelper.phoneHome(InspectorTaskDescriptor.BLACK_DUCK_INSPECTOR_TASK_ID);
        } catch (IntegrationException | IllegalStateException e) {
            logger.error(String.format("Black Duck server config invalid. %s", e.getMessage()), e);
            exceptionMessage = e.getMessage();
        }
        for (Repository foundRepository : commonTaskFilters.findRelevantRepositories(repository)) {
            if (commonTaskFilters.isProxyRepository(foundRepository.getType())) {
                Optional<DependencyType> dependencyTypeOptional = dependencyGenerator.findDependency(foundRepository.getFormat().getValue());
                if (!dependencyTypeOptional.isPresent()) {
                    throw new TaskInterruptedException("Task being run on unsupported repository", true);
                }
                DependencyType dependencyType = dependencyTypeOptional.get();
                InspectorConfiguration inspectorConfiguration;
                if (StringUtils.isNotBlank(exceptionMessage)) {
                    inspectorConfiguration = InspectorConfiguration.createConfigurationWithError(exceptionMessage, repository, dependencyType);
                } else {
                    inspectorConfiguration = InspectorConfiguration.createConfiguration(repository, dependencyType, blackDuckService, projectService, codeLocationCreationService, bdioUploadService, componentService);
                }
                InspectorScanner inspectorScanner = new InspectorScanner(commonRepositoryTaskHelper, dateTimeParser, dependencyGenerator, inspectorMetaDataProcessor, commonTaskFilters, taskConfiguration(), inspectorConfiguration);
                inspectorScanner.inspectRepository();
            }
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
        return commonRepositoryTaskHelper.getTaskMessage(InspectorTaskDescriptor.BLACK_DUCK_INSPECTOR_TASK_NAME, getRepositoryField());
    }

}
