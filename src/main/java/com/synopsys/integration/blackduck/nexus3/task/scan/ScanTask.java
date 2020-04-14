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
package com.synopsys.integration.blackduck.nexus3.task.scan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatchRunner;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.SignatureScannerService;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonTaskFilters;
import com.synopsys.integration.blackduck.rest.BlackDuckHttpClient;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.phonehome.PhoneHomeResponse;
import com.synopsys.integration.util.IntEnvironmentVariables;
import com.synopsys.integration.util.NoThreadExecutorService;

@Named
public class ScanTask extends RepositoryTaskSupport {
    public static final String SCAN_CODE_LOCATION_NAME = "Nexus3Scan";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final QueryManager queryManager;
    private final DateTimeParser dateTimeParser;
    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final ScanMetaDataProcessor scanMetaDataProcessor;
    private final CommonTaskFilters commonTaskFilters;

    @Inject
    public ScanTask(QueryManager queryManager, DateTimeParser dateTimeParser, CommonRepositoryTaskHelper commonRepositoryTaskHelper, ScanMetaDataProcessor scanMetaDataProcessor,
        CommonTaskFilters commonTaskFilters) {
        this.queryManager = queryManager;
        this.dateTimeParser = dateTimeParser;
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.scanMetaDataProcessor = scanMetaDataProcessor;
        this.commonTaskFilters = commonTaskFilters;
    }

    @Override
    protected boolean appliesTo(Repository repository) {
        return commonTaskFilters.doesRepositoryApply(repository, getRepositoryField());
    }

    @Override
    public String getMessage() {
        return commonRepositoryTaskHelper.getTaskMessage(ScanTaskDescriptor.BLACK_DUCK_SCAN_TASK_NAME, getRepositoryField());
    }

    @Override
    protected void execute(Repository repository) {
        IntLogger intLogger = new Slf4jIntLogger(logger);
        Optional<PhoneHomeResponse> phoneHomeResponse = commonRepositoryTaskHelper.phoneHome(ScanTaskDescriptor.BLACK_DUCK_SCAN_TASK_ID);

        String exceptionMessage = null;
        BlackDuckServerConfig blackDuckServerConfig = null;
        SignatureScannerService signatureScannerService = null;
        CodeLocationCreationService codeLocationCreationService = null;
        BlackDuckService blackDuckService = null;
        ProjectService projectService = null;
        try {
            blackDuckServerConfig = commonRepositoryTaskHelper.getBlackDuckServerConfig();
            BlackDuckServicesFactory blackDuckServicesFactory = commonRepositoryTaskHelper.getBlackDuckServicesFactory();

            IntEnvironmentVariables intEnvironmentVariables = new IntEnvironmentVariables();
            BlackDuckHttpClient blackDuckHttpClient = blackDuckServerConfig.createBlackDuckHttpClient(intLogger);

            signatureScannerService = blackDuckServicesFactory.createSignatureScannerService(ScanBatchRunner.createDefault(intLogger, blackDuckHttpClient, intEnvironmentVariables, new NoThreadExecutorService()));
            codeLocationCreationService = blackDuckServicesFactory.createCodeLocationCreationService();
            blackDuckService = blackDuckServicesFactory.createBlackDuckService();
            projectService = blackDuckServicesFactory.createProjectService();
        } catch (IntegrationException | IllegalStateException e) {
            logger.error(String.format("Black Duck hub server config invalid. %s", e.getMessage()), e);
            exceptionMessage = e.getMessage();
        }

        File workingDirectory = commonRepositoryTaskHelper.getWorkingDirectory(taskConfiguration());
        File workingBlackDuckDirectory = new File(workingDirectory, "blackduck");
        File tempFileStorage = new File(workingBlackDuckDirectory, "temp");
        File outputDirectory = new File(workingBlackDuckDirectory, "output");
        try {
            Files.createDirectories(tempFileStorage.toPath());
            Files.createDirectories(outputDirectory.toPath());
        } catch (IOException e) {
            intLogger.debug(e.getMessage(), e);
            throw new TaskInterruptedException("Could not create directories to use with Scanner: " + e.getMessage(), true);
        }

        boolean alwaysScan = taskConfiguration().getBoolean(ScanTaskDescriptor.KEY_ALWAYS_CHECK, false);
        boolean redoFailures = taskConfiguration().getBoolean(ScanTaskDescriptor.KEY_REDO_FAILURES, false);
        for (Repository foundRepository : commonTaskFilters.findRelevantRepositories(repository)) {
            if (commonTaskFilters.isHostedRepository(foundRepository.getType())) {
                ScanConfiguration scanConfiguration;
                if (StringUtils.isNotBlank(exceptionMessage)) {
                    scanConfiguration = ScanConfiguration.createConfigurationWithError(exceptionMessage, repository, alwaysScan, redoFailures);
                } else {
                    scanConfiguration = ScanConfiguration.createConfiguration(repository, alwaysScan, redoFailures, blackDuckServerConfig, signatureScannerService, codeLocationCreationService, blackDuckService, projectService,
                        workingBlackDuckDirectory, tempFileStorage, outputDirectory);
                }
                RepositoryScanner repositoryScanner = new RepositoryScanner(queryManager, dateTimeParser, scanMetaDataProcessor, taskConfiguration(), commonRepositoryTaskHelper, commonTaskFilters, scanConfiguration);
                repositoryScanner.scanRepository();
            }
        }
        if (phoneHomeResponse.isPresent()) {
            commonRepositoryTaskHelper.endPhoneHome(phoneHomeResponse.get());
        } else {
            logger.debug("Could not phone home.");
        }
    }

}
