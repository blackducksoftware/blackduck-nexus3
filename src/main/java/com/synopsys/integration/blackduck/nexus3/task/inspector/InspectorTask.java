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

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonTaskFilters;
import com.synopsys.integration.blackduck.nexus3.task.inspector.dependency.DependencyGenerator;
import com.synopsys.integration.blackduck.nexus3.task.inspector.dependency.DependencyType;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.CodeLocationService;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.ProjectRequestBuilder;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.SimpleBdioFactory;
import com.synopsys.integration.hub.bdio.graph.MutableDependencyGraph;
import com.synopsys.integration.hub.bdio.model.Forge;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.hub.bdio.model.dependency.Dependency;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalId;
import com.synopsys.integration.util.IntegrationEscapeUtil;

@Named
public class InspectorTask extends RepositoryTaskSupport {
    public static final String INSPECTOR_CODE_LOCATION_NAME = "Nexus3Inspection";
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
        final Optional<DependencyType> dependencyType = dependencyGenerator.findDependency(repository.getFormat());
        if (!dependencyType.isPresent()) {
            throw new TaskInterruptedException("Task being run on unsupported repository", true);
        }

        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
        final MutableDependencyGraph mutableDependencyGraph = simpleBdioFactory.createMutableDependencyGraph();
        final Map<String, AssetWrapper> assetWrapperMap = new HashMap<>();

        final Query pagedQuery = commonRepositoryTaskHelper.createPagedQuery(Optional.empty()).build();
        PagedResult<Asset> filteredAssets = commonRepositoryTaskHelper.retrievePagedAssets(repository, pagedQuery);
        boolean resultsFound = false;
        while (filteredAssets.hasResults()) {
            logger.info("Found some items from the DB");
            for (final Asset asset : filteredAssets.getTypeList()) {
                final AssetWrapper assetWrapper = new AssetWrapper(asset, repository, commonRepositoryTaskHelper.getQueryManager());
                final String name = assetWrapper.getFullPath();
                final boolean hasBeenModified = processAsset(assetWrapper, dependencyType.get(), mutableDependencyGraph, assetWrapperMap);
                resultsFound = resultsFound || hasBeenModified;
                if (resultsFound) {
                    logger.info("Found new item {}, adding to Black Duck.", name);
                }
            }

            final Query nextPage = commonRepositoryTaskHelper.createPagedQuery(filteredAssets.getLastName()).build();
            filteredAssets = commonRepositoryTaskHelper.retrievePagedAssets(repository, nextPage);
        }

        if (resultsFound) {
            logger.info("Creating Black Duck project.");
            uploadToBlackDuck(repository, mutableDependencyGraph, simpleBdioFactory, dependencyType.get(), assetWrapperMap);
        } else {
            logger.warn("No new assets found with set criteria.");
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

        final boolean modified = commonTaskFilters.hasAssetBeenModified(assetWrapper);
        final String originId = dependency.externalId.createHubOriginId();
        assetWrapper.addPendingToBlackDuckPanel("Asset waiting to be uploaded to Black Duck.");
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.ASSET_ORIGIN_ID, originId);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
        assetWrapper.updateAsset();
        logger.debug("Adding asset to map with originId as key: {}", originId);
        assetWrapperMap.put(originId, assetWrapper);
        return modified;
    }

    private void uploadToBlackDuck(final Repository repository, final MutableDependencyGraph mutableDependencyGraph, final SimpleBdioFactory simpleBdioFactory, final DependencyType dependencyType,
        final Map<String, AssetWrapper> assetWrapperMap) {
        final Forge nexusForge = new Forge("/", "/", "nexus");
        final String projectName = repository.getName();
        final ProjectVersionView projectVersionView;
        final String codeLocationName = String.join("/", INSPECTOR_CODE_LOCATION_NAME, projectName, dependencyType.getRepositoryType());
        try {
            logger.debug("Creating project in Black Duck if needed: {}", projectName);
            final HubServicesFactory hubServicesFactory = commonRepositoryTaskHelper.getHubServicesFactory();
            final ProjectService projectService = hubServicesFactory.createProjectService();
            final ProjectRequestBuilder projectRequestBuilder = new ProjectRequestBuilder();
            projectRequestBuilder.setProjectName(projectName);
            projectRequestBuilder.setVersionName(INSPECTOR_CODE_LOCATION_NAME);
            final ProjectVersionWrapper projectVersionWrapper = projectService.getProjectVersionAndCreateIfNeeded(projectRequestBuilder.buildObject());
            projectVersionView = projectVersionWrapper.getProjectVersionView();
            final ExternalId projectRoot = simpleBdioFactory.createNameVersionExternalId(nexusForge, projectName, INSPECTOR_CODE_LOCATION_NAME);
            final SimpleBdioDocument simpleBdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, projectName, INSPECTOR_CODE_LOCATION_NAME, projectRoot, mutableDependencyGraph);
            sendInspectorData(simpleBdioDocument, simpleBdioFactory);
            final String uploadUrl = commonRepositoryTaskHelper.verifyUpload(codeLocationName, projectVersionView);
            final TaskStatus status = uploadUrl.startsWith(CommonRepositoryTaskHelper.VERIFICATION_ERROR) ? TaskStatus.FAILURE : TaskStatus.SUCCESS;
            inspectorMetaDataProcessor.updateRepositoryMetaData(projectVersionView, assetWrapperMap, status);
        } catch (final IntegrationException e) {
            logger.debug("Issue communicating with BlackDuck: {}", e.getMessage());
            updateErrorStatus(assetWrapperMap.values(), e.getMessage());
            throw new TaskInterruptedException("Issue communicating with BlackDuck", true);
        } catch (final IOException e) {
            logger.error("Error writing to file: {}", e.getMessage());
            updateErrorStatus(assetWrapperMap.values(), e.getMessage());
            throw new TaskInterruptedException("Couldn't save inspection data", true);
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

    private void sendInspectorData(final SimpleBdioDocument bdioDocument, final SimpleBdioFactory simpleBdioFactory) throws IntegrationException, IOException {
        final HubServicesFactory hubServicesFactory = commonRepositoryTaskHelper.getHubServicesFactory();
        final CodeLocationService codeLocationService = hubServicesFactory.createCodeLocationService();

        final IntegrationEscapeUtil integrationEscapeUtil = new IntegrationEscapeUtil();
        final File workingDirectory = commonRepositoryTaskHelper.getWorkingDirectory(taskConfiguration());
        final File blackDuckWorkingDirectory = new File(workingDirectory, "inspector");
        blackDuckWorkingDirectory.mkdirs();
        final File bdioFile = new File(blackDuckWorkingDirectory, integrationEscapeUtil.escapeForUri(bdioDocument.billOfMaterials.spdxName));
        bdioFile.delete();
        bdioFile.createNewFile();
        logger.debug("Sending data to BlackDuck.");
        simpleBdioFactory.writeSimpleBdioDocumentToFile(bdioFile, bdioDocument);

        codeLocationService.importBomFile(bdioFile);
        bdioFile.delete();
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
