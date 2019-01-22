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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
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
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationData;
import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationService;
import com.synopsys.integration.blackduck.codelocation.bdioupload.BdioUploadCodeLocationCreationRequest;
import com.synopsys.integration.blackduck.codelocation.bdioupload.BdioUploadService;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadBatch;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadBatchOutput;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadTarget;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonTaskFilters;
import com.synopsys.integration.blackduck.nexus3.task.inspector.dependency.DependencyGenerator;
import com.synopsys.integration.blackduck.nexus3.task.inspector.dependency.DependencyType;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
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
    public InspectorTask(final CommonRepositoryTaskHelper commonRepositoryTaskHelper, final DateTimeParser dateTimeParser, final DependencyGenerator dependencyGenerator, final InspectorMetaDataProcessor inspectorMetaDataProcessor,
        final CommonTaskFilters commonTaskFilters) {
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.dateTimeParser = dateTimeParser;
        this.dependencyGenerator = dependencyGenerator;
        this.inspectorMetaDataProcessor = inspectorMetaDataProcessor;
        this.commonTaskFilters = commonTaskFilters;
    }

    @Override
    protected void execute(final Repository repository) {
        String exceptionMessage = null;
        ProjectService projectService = null;
        CodeLocationCreationService codeLocationCreationService = null;
        BdioUploadService bdioUploadService = null;
        String blackDuckUrl = null;
        int timeOut = -1;
        Optional<PhoneHomeResponse> phoneHomeResponse = Optional.empty();
        try {
            final BlackDuckServicesFactory blackDuckServicesFactory = commonRepositoryTaskHelper.getBlackDuckServicesFactory();
            projectService = blackDuckServicesFactory.createProjectService();
            codeLocationCreationService = blackDuckServicesFactory.createCodeLocationCreationService();
            bdioUploadService = blackDuckServicesFactory.createBdioUploadService();
            final BlackDuckServerConfig blackDuckServerConfig = commonRepositoryTaskHelper.getBlackDuckServerConfig();
            blackDuckUrl = blackDuckServerConfig.getBlackDuckUrl().toString();
            timeOut = blackDuckServerConfig.getTimeout();

            phoneHomeResponse = commonRepositoryTaskHelper.phoneHome(InspectorTaskDescriptor.BLACK_DUCK_INSPECTOR_TASK_ID);
        } catch (final IntegrationException | IllegalStateException e) {
            logger.error("Black Duck server config invalid. " + e.getMessage(), e);
            exceptionMessage = e.getMessage();
        }
        for (final Repository foundRepository : commonTaskFilters.findRelevantRepositories(repository)) {
            if (commonTaskFilters.isProxyRepository(foundRepository.getType())) {
                final Optional<DependencyType> dependencyType = dependencyGenerator.findDependency(foundRepository.getFormat().getValue());
                if (!dependencyType.isPresent()) {
                    throw new TaskInterruptedException("Task being run on unsupported repository", true);
                }

                final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
                final MutableDependencyGraph mutableDependencyGraph = simpleBdioFactory.createMutableDependencyGraph();
                final Map<String, AssetWrapper> assetWrapperMap = new HashMap<>();

                final String repoName = foundRepository.getName();
                logger.info("Checking repository for assets: {}", repoName);
                final Query pagedQuery = commonRepositoryTaskHelper.createPagedQuery(Optional.empty()).build();
                PagedResult<Asset> filteredAssets = commonRepositoryTaskHelper.retrievePagedAssets(foundRepository, pagedQuery);
                boolean uploadToBlackDuck = false;
                while (filteredAssets.hasResults()) {
                    logger.info("Found some items from the DB");
                    for (final Asset asset : filteredAssets.getTypeList()) {
                        final AssetWrapper assetWrapper = AssetWrapper.createInspectionAssetWrapper(asset, foundRepository, commonRepositoryTaskHelper.getQueryManager());

                        if (StringUtils.isNotBlank(exceptionMessage)) {
                            commonRepositoryTaskHelper.failedConnection(assetWrapper, exceptionMessage);
                            assetWrapper.updateAsset();
                        } else {
                            final boolean shouldProcessAsset = processAsset(assetWrapper, dependencyType.get(), mutableDependencyGraph, assetWrapperMap);
                            if (shouldProcessAsset) {
                                // Only set resultsFound to true, if you set it to false you risk falsely reporting that there are no new assets
                                // I believe this can be improved upon... -BM
                                uploadToBlackDuck = true;
                            }
                        }
                    }

                    final Query nextPage = commonRepositoryTaskHelper.createPagedQuery(filteredAssets.getLastName()).build();
                    filteredAssets = commonRepositoryTaskHelper.retrievePagedAssets(foundRepository, nextPage);
                }

                if (uploadToBlackDuck && null != projectService && null != codeLocationCreationService && null != bdioUploadService) {
                    logger.info("Creating Black Duck project.");
                    uploadToBlackDuck(projectService, codeLocationCreationService, bdioUploadService, blackDuckUrl, timeOut, repoName, mutableDependencyGraph, simpleBdioFactory, dependencyType.get(), assetWrapperMap);
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

    private boolean processAsset(final AssetWrapper assetWrapper, final DependencyType dependencyType, final MutableDependencyGraph mutableDependencyGraph, final Map<String, AssetWrapper> assetWrapperMap) {
        final String name = assetWrapper.getName();
        final String version = assetWrapper.getVersion();

        if (commonTaskFilters.skipAssetProcessing(assetWrapper, taskConfiguration())) {
            logger.debug("Binary file did not meet requirements for inspection: {}", name);
            return false;
        }

        final Dependency dependency = dependencyGenerator.createDependency(dependencyType, name, version, assetWrapper.getAsset().attributes());
        logger.info("Created new dependency: {}", dependency);
        mutableDependencyGraph.addChildToRoot(dependency);

        final String originId = dependency.externalId.createBlackDuckOriginId();
        assetWrapper.addPendingToBlackDuckPanel("Asset waiting to be uploaded to Black Duck.");
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.ASSET_ORIGIN_ID, originId);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
        assetWrapper.updateAsset();
        logger.debug("Adding asset to map with originId as key: {}", originId);
        assetWrapperMap.put(originId, assetWrapper);
        return true;
    }

    private void uploadToBlackDuck(final ProjectService projectService, final CodeLocationCreationService codeLocationCreationService, final BdioUploadService bdioUploadService, final String blackDuckUrl, final int timeOutInSeconds,
        final String repositoryName,
        final MutableDependencyGraph mutableDependencyGraph, final SimpleBdioFactory simpleBdioFactory,
        final DependencyType dependencyType, final Map<String, AssetWrapper> assetWrapperMap) {
        final Forge nexusForge = new Forge("/", "/", "nexus");
        final ProjectVersionView projectVersionView;
        final String codeLocationName = String.join("/", INSPECTOR_VERSION_NAME, repositoryName, dependencyType.getRepositoryType());
        try {
            logger.debug("Creating project in Black Duck if needed: {}", repositoryName);
            projectVersionView = inspectorMetaDataProcessor.getOrCreateProjectVersion(projectService, repositoryName);

            final ExternalId projectRoot = simpleBdioFactory.createNameVersionExternalId(nexusForge, repositoryName, INSPECTOR_VERSION_NAME);
            final SimpleBdioDocument simpleBdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, repositoryName, INSPECTOR_VERSION_NAME, projectRoot, mutableDependencyGraph);

            final CodeLocationCreationData<UploadBatchOutput> uploadData = sendInspectorData(bdioUploadService, simpleBdioDocument, simpleBdioFactory, codeLocationName);

            final Set<String> successfulCodeLocationNames = uploadData.getOutput().getSuccessfulCodeLocationNames();
            if (successfulCodeLocationNames.contains(codeLocationName)) {
                codeLocationCreationService.waitForCodeLocations(uploadData.getNotificationTaskRange(), successfulCodeLocationNames, timeOutInSeconds * 5);
                inspectorMetaDataProcessor.updateRepositoryMetaData(projectService, blackDuckUrl, projectVersionView, assetWrapperMap, TaskStatus.SUCCESS);
            } else {
                inspectorMetaDataProcessor.updateRepositoryMetaData(projectService, blackDuckUrl, projectVersionView, assetWrapperMap, TaskStatus.FAILURE);
            }
        } catch (final IntegrationException e) {
            logger.error("Issue communicating with Black Duck: " + e.getMessage(), e);
            updateErrorStatus(assetWrapperMap.values(), e.getMessage());
            throw new TaskInterruptedException("Issue communicating with Black Duck", true);
        } catch (final IOException e) {
            logger.error("Error writing to file: {}", e.getMessage());
            updateErrorStatus(assetWrapperMap.values(), e.getMessage());
            throw new TaskInterruptedException("Couldn't save inspection data", true);
        } catch (final InterruptedException e) {
            logger.error("Waiting for the results from Black Duck was interrupted: {}", e.getMessage());
            updateErrorStatus(assetWrapperMap.values(), e.getMessage());
            throw new TaskInterruptedException("Waiting for Black Duck results interrupted", true);
        }
    }

    private void updateErrorStatus(final Collection<AssetWrapper> assetWrappers, final String error) {
        for (final AssetWrapper assetWrapper : assetWrappers) {
            assetWrapper.removeAllBlackDuckData();
            assetWrapper.addFailureToBlackDuckPanel(error);
            assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
            assetWrapper.updateAsset();
        }
    }

    private CodeLocationCreationData<UploadBatchOutput> sendInspectorData(final BdioUploadService bdioUploadService, final SimpleBdioDocument bdioDocument, final SimpleBdioFactory simpleBdioFactory, final String codeLocationName)
        throws IntegrationException, IOException {

        final IntegrationEscapeUtil integrationEscapeUtil = new IntegrationEscapeUtil();
        final File workingDirectory = commonRepositoryTaskHelper.getWorkingDirectory(taskConfiguration());
        final File blackDuckWorkingDirectory = new File(workingDirectory, "inspector");
        blackDuckWorkingDirectory.mkdirs();
        final File bdioFile = new File(blackDuckWorkingDirectory, integrationEscapeUtil.escapeForUri(codeLocationName));
        bdioFile.delete();
        bdioFile.createNewFile();
        logger.debug("Sending data to Black Duck.");
        simpleBdioFactory.writeSimpleBdioDocumentToFile(bdioFile, bdioDocument);

        final UploadBatch uploadBatch = new UploadBatch();
        uploadBatch.addUploadTarget(UploadTarget.createDefault(codeLocationName, bdioFile));

        final BdioUploadCodeLocationCreationRequest uploadRequest = bdioUploadService.createUploadRequest(uploadBatch);
        final CodeLocationCreationData<UploadBatchOutput> uploadBatchOutputCodeLocationCreationData = bdioUploadService.uploadBdio(uploadRequest);

        bdioFile.delete();
        return uploadBatchOutputCodeLocationCreationData;
    }

    @Override
    protected boolean appliesTo(final Repository repository) {
        return commonRepositoryTaskHelper.doesRepositoryApply(repository, getRepositoryField());
    }

    @Override
    public String getMessage() {
        return commonRepositoryTaskHelper.getTaskMessage(InspectorTaskDescriptor.BLACK_DUCK_INSPECTOR_TASK_NAME, getRepositoryField());
    }

}
