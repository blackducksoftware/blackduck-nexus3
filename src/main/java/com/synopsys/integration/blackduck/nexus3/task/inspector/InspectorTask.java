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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.bdio.SimpleBdioFactory;
import com.synopsys.integration.bdio.graph.MutableDependencyGraph;
import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.bdio.model.dependency.Dependency;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.api.generated.view.ComponentSearchResultView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationData;
import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationService;
import com.synopsys.integration.blackduck.codelocation.CodeLocationWaitResult;
import com.synopsys.integration.blackduck.codelocation.bdioupload.BdioUploadCodeLocationCreationRequest;
import com.synopsys.integration.blackduck.codelocation.bdioupload.BdioUploadService;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadBatch;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadBatchOutput;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadTarget;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.exception.BlackDuckApiException;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonTaskFilters;
import com.synopsys.integration.blackduck.nexus3.task.inspector.dependency.DependencyGenerator;
import com.synopsys.integration.blackduck.nexus3.task.inspector.dependency.DependencyType;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.ComponentService;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.phonehome.PhoneHomeResponse;
import com.synopsys.integration.util.IntegrationEscapeUtil;

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
        String blackDuckUrl = null;
        int timeOut = -1;
        Optional<PhoneHomeResponse> phoneHomeResponse = Optional.empty();
        try {
            BlackDuckServicesFactory blackDuckServicesFactory = commonRepositoryTaskHelper.getBlackDuckServicesFactory();
            blackDuckService = blackDuckServicesFactory.createBlackDuckService();
            projectService = blackDuckServicesFactory.createProjectService();
            codeLocationCreationService = blackDuckServicesFactory.createCodeLocationCreationService();
            bdioUploadService = blackDuckServicesFactory.createBdioUploadService();
            componentService = blackDuckServicesFactory.createComponentService();
            BlackDuckServerConfig blackDuckServerConfig = commonRepositoryTaskHelper.getBlackDuckServerConfig();
            blackDuckUrl = blackDuckServerConfig.getBlackDuckUrl().toString();
            timeOut = blackDuckServerConfig.getTimeout();

            phoneHomeResponse = commonRepositoryTaskHelper.phoneHome(InspectorTaskDescriptor.BLACK_DUCK_INSPECTOR_TASK_ID);
        } catch (IntegrationException | IllegalStateException e) {
            logger.error("Black Duck server config invalid. " + e.getMessage(), e);
            exceptionMessage = e.getMessage();
        }
        for (Repository foundRepository : commonTaskFilters.findRelevantRepositories(repository)) {
            if (commonTaskFilters.isProxyRepository(foundRepository.getType())) {
                Optional<DependencyType> dependencyType = dependencyGenerator.findDependency(foundRepository.getFormat().getValue());
                if (!dependencyType.isPresent()) {
                    throw new TaskInterruptedException("Task being run on unsupported repository", true);
                }

                SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
                MutableDependencyGraph mutableDependencyGraph = simpleBdioFactory.createMutableDependencyGraph();
                Map<String, AssetWrapper> assetWrapperMap = new HashMap<>();

                String repoName = foundRepository.getName();
                logger.info("Checking repository for assets: {}", repoName);
                Query pagedQuery = commonRepositoryTaskHelper.createPagedQuery(Optional.empty()).build();
                PagedResult<Asset> filteredAssets = commonRepositoryTaskHelper.retrievePagedAssets(foundRepository, pagedQuery);
                boolean uploadToBlackDuck = false;
                while (filteredAssets.hasResults()) {
                    logger.info("Found some items from the DB");
                    for (Asset asset : filteredAssets.getTypeList()) {
                        AssetWrapper assetWrapper = AssetWrapper.createInspectionAssetWrapper(asset, foundRepository, commonRepositoryTaskHelper.getQueryManager());

                        if (StringUtils.isNotBlank(exceptionMessage)) {
                            commonRepositoryTaskHelper.failedConnection(assetWrapper, exceptionMessage);
                            assetWrapper.updateAsset();
                        } else {
                            boolean shouldProcessAsset = processAsset(componentService, assetWrapper, dependencyType.get(), mutableDependencyGraph, assetWrapperMap);
                            if (shouldProcessAsset) {
                                // Only set resultsFound to true, if you set it to false you risk falsely reporting that there are no new assets
                                // I believe this can be improved upon... -BM
                                uploadToBlackDuck = true;
                            }
                        }
                    }

                    Query nextPage = commonRepositoryTaskHelper.createPagedQuery(filteredAssets.getLastName()).build();
                    filteredAssets = commonRepositoryTaskHelper.retrievePagedAssets(foundRepository, nextPage);
                }

                if (uploadToBlackDuck && null != projectService && null != codeLocationCreationService && null != bdioUploadService && null != blackDuckService) {
                    logger.info("Creating Black Duck project.");
                    uploadToBlackDuck(blackDuckService, projectService, codeLocationCreationService, bdioUploadService, blackDuckUrl, timeOut, repoName, mutableDependencyGraph, simpleBdioFactory, dependencyType.get(), assetWrapperMap);
                } else {
                    logger.warn("Won't upload to Black Duck as no items were processed.");
                }
            }
        }
        if (phoneHomeResponse.isPresent()) {
            commonRepositoryTaskHelper.endPhoneHome(phoneHomeResponse.get());
        } else {
            logger.debug("Could not phone home.");
        }
    }

    private boolean processAsset(ComponentService componentService, AssetWrapper assetWrapper, DependencyType dependencyType, MutableDependencyGraph mutableDependencyGraph,
        Map<String, AssetWrapper> assetWrapperMap) {
        String name = assetWrapper.getName();
        String version = assetWrapper.getVersion();

        DateTime lastModified = assetWrapper.getAssetLastUpdated();
        String fullPathName = assetWrapper.getFullPath();
        String fileName = null;
        try {
            fileName = assetWrapper.getFilename();
        } catch (IntegrationException e) {
            logger.debug("Skipping asset: {}. {}", name, e.getMessage());
        }

        if (commonTaskFilters.skipAssetProcessing(lastModified, fullPathName, fileName, taskConfiguration())) {
            logger.debug("Binary file did not meet requirements for inspection: {}", name);
            return false;
        }
        ExternalId externalId = dependencyGenerator.createExternalId(dependencyType, name, version, assetWrapper.getAsset().attributes());
        String originId = null;
        try {
            Optional<ComponentSearchResultView> firstOrEmptyResult = componentService.getFirstOrEmptyResult(externalId);
            if (firstOrEmptyResult.isPresent()) {
                originId = firstOrEmptyResult.get().getOriginId();
                Dependency dependency = dependencyGenerator.createDependency(name, version, externalId);
                logger.info("Created new dependency: {}", dependency);
                mutableDependencyGraph.addChildToRoot(dependency);

                assetWrapper.addPendingToBlackDuckPanel("Asset waiting to be uploaded to Black Duck.");
                assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.ASSET_ORIGIN_ID, originId);
                assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());

                logger.debug("Adding asset to map with originId as key: {}", originId);
                assetWrapperMap.put(originId, assetWrapper);
            } else {
                assetWrapper.addComponentNotFoundToBlackDuckPanel(String.format("Could not find this component %s in Black Duck.", externalId.createExternalId()));
            }
        } catch (IntegrationException e) {
            assetWrapper.addFailureToBlackDuckPanel(String.format("Something went wrong communicating with Black Duck: %s", e.getMessage()));

        }
        assetWrapper.updateAsset();
        return true;
    }

    private void uploadToBlackDuck(BlackDuckService blackDuckService, ProjectService projectService, CodeLocationCreationService codeLocationCreationService, BdioUploadService bdioUploadService,
        String blackDuckUrl, int timeOutInSeconds, String repositoryName, MutableDependencyGraph mutableDependencyGraph, SimpleBdioFactory simpleBdioFactory, DependencyType dependencyType,
        Map<String, AssetWrapper> assetWrapperMap) {
        Forge nexusForge = new Forge("/", "nexus");
        ProjectVersionView projectVersionView;
        String codeLocationName = String.join("/", INSPECTOR_VERSION_NAME, repositoryName, dependencyType.getRepositoryType());
        try {
            logger.debug("Creating project in Black Duck if needed: {}", repositoryName);
            projectVersionView = inspectorMetaDataProcessor.getOrCreateProjectVersion(blackDuckService, projectService, repositoryName);

            ExternalId projectRoot = simpleBdioFactory.createNameVersionExternalId(nexusForge, repositoryName, INSPECTOR_VERSION_NAME);
            SimpleBdioDocument simpleBdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, repositoryName, INSPECTOR_VERSION_NAME, projectRoot, mutableDependencyGraph);

            CodeLocationCreationData<UploadBatchOutput> uploadData = sendInspectorData(bdioUploadService, simpleBdioDocument, simpleBdioFactory, codeLocationName);

            Set<String> successfulCodeLocationNames = uploadData.getOutput().getSuccessfulCodeLocationNames();
            CodeLocationWaitResult.Status status = CodeLocationWaitResult.Status.PARTIAL;
            if (successfulCodeLocationNames.contains(codeLocationName)) {
                CodeLocationWaitResult codeLocationWaitResult = codeLocationCreationService
                                                                    .waitForCodeLocations(uploadData.getNotificationTaskRange(), successfulCodeLocationNames, successfulCodeLocationNames.size(), timeOutInSeconds * 5L);
                status = codeLocationWaitResult.getStatus();
            }
            if (CodeLocationWaitResult.Status.COMPLETE == status) {
                inspectorMetaDataProcessor.updateRepositoryMetaData(blackDuckService, blackDuckUrl, projectVersionView, assetWrapperMap, TaskStatus.SUCCESS);
            } else {
                inspectorMetaDataProcessor.updateRepositoryMetaData(blackDuckService, blackDuckUrl, projectVersionView, assetWrapperMap, TaskStatus.FAILURE);
            }
        } catch (BlackDuckApiException e) {
            logger.error("Problem communicating with Black Duck: {}", e.getMessage());
            updateErrorStatus(assetWrapperMap.values(), e.getMessage());
            throw new TaskInterruptedException("Problem communicating with Black Duck", true);
        } catch (IntegrationException e) {
            logger.error("Issue communicating with Black Duck: " + e.getMessage(), e);
            updateErrorStatus(assetWrapperMap.values(), e.getMessage());
            throw new TaskInterruptedException("Issue communicating with Black Duck", true);
        } catch (IOException e) {
            logger.error("Error writing to file: {}", e.getMessage());
            updateErrorStatus(assetWrapperMap.values(), e.getMessage());
            throw new TaskInterruptedException("Couldn't save inspection data", true);
        } catch (InterruptedException e) {
            logger.error("Waiting for the results from Black Duck was interrupted: {}", e.getMessage());
            updateErrorStatus(assetWrapperMap.values(), e.getMessage());
            Thread.currentThread().interrupt();
            throw new TaskInterruptedException("Waiting for Black Duck results interrupted", true);
        }
    }

    private void updateErrorStatus(Collection<AssetWrapper> assetWrappers, String error) {
        for (AssetWrapper assetWrapper : assetWrappers) {
            assetWrapper.removeAllBlackDuckData();
            assetWrapper.addFailureToBlackDuckPanel(error);
            assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
            assetWrapper.updateAsset();
        }
    }

    private CodeLocationCreationData<UploadBatchOutput> sendInspectorData(BdioUploadService bdioUploadService, SimpleBdioDocument bdioDocument, SimpleBdioFactory simpleBdioFactory, String codeLocationName)
        throws IntegrationException, IOException {

        IntegrationEscapeUtil integrationEscapeUtil = new IntegrationEscapeUtil();
        File workingDirectory = commonRepositoryTaskHelper.getWorkingDirectory(taskConfiguration());
        File blackDuckWorkingDirectory = new File(workingDirectory, "inspector");
        if (blackDuckWorkingDirectory.mkdirs()) {
            logger.debug("Created directories for {}", blackDuckWorkingDirectory.getAbsolutePath());
        } else {
            logger.debug("Directories {} already exists", blackDuckWorkingDirectory.getAbsolutePath());
        }

        File bdioFile = new File(blackDuckWorkingDirectory, integrationEscapeUtil.escapeForUri(codeLocationName));
        Files.delete(bdioFile.toPath());
        if (bdioFile.createNewFile()) {
            logger.debug("Created file {}", bdioFile.getAbsolutePath());
        } else {
            logger.debug("File {} already exists", bdioFile.getAbsolutePath());
        }

        logger.debug("Sending data to Black Duck.");
        simpleBdioFactory.writeSimpleBdioDocumentToFile(bdioFile, bdioDocument);

        UploadBatch uploadBatch = new UploadBatch();
        uploadBatch.addUploadTarget(UploadTarget.createDefault(codeLocationName, bdioFile));

        BdioUploadCodeLocationCreationRequest uploadRequest = bdioUploadService.createUploadRequest(uploadBatch);
        CodeLocationCreationData<UploadBatchOutput> uploadBatchOutputCodeLocationCreationData = bdioUploadService.uploadBdio(uploadRequest);

        Files.delete(bdioFile.toPath());
        return uploadBatchOutputCodeLocationCreationData;
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
