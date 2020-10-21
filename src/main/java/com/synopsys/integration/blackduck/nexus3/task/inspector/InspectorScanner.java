package com.synopsys.integration.blackduck.nexus3.task.inspector;

import java.util.Optional;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionComponentView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
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
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.ProjectBomService;
import com.synopsys.integration.exception.IntegrationException;

public class InspectorScanner {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final DateTimeParser dateTimeParser;
    private final DependencyGenerator dependencyGenerator;
    private final InspectorMetaDataProcessor inspectorMetaDataProcessor;
    private final CommonTaskFilters commonTaskFilters;
    private final TaskConfiguration taskConfiguration;
    private final InspectorConfiguration inspectorConfiguration;

    public InspectorScanner(CommonRepositoryTaskHelper commonRepositoryTaskHelper, DateTimeParser dateTimeParser, DependencyGenerator dependencyGenerator,
        InspectorMetaDataProcessor inspectorMetaDataProcessor, CommonTaskFilters commonTaskFilters, TaskConfiguration taskConfiguration, InspectorConfiguration inspectorConfiguration) {
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.dateTimeParser = dateTimeParser;
        this.dependencyGenerator = dependencyGenerator;
        this.inspectorMetaDataProcessor = inspectorMetaDataProcessor;
        this.commonTaskFilters = commonTaskFilters;
        this.taskConfiguration = taskConfiguration;
        this.inspectorConfiguration = inspectorConfiguration;
    }

    public void inspectRepository() {
        String repositoryName = inspectorConfiguration.getRepository().getName();
        logger.info("Checking repository for assets: {}", repositoryName);
        Query pagedQuery = commonRepositoryTaskHelper.createPagedQuery(Optional.empty()).build();
        PagedResult<Asset> filteredAssets = commonRepositoryTaskHelper.retrievePagedAssets(inspectorConfiguration.getRepository(), pagedQuery);
        ProjectVersionView projectVersionView;
        try {
            logger.debug("Creating Project Version in Black Duck: {}", repositoryName);
            projectVersionView = inspectorMetaDataProcessor.getOrCreateProjectVersion(inspectorConfiguration.getBlackDuckService(), inspectorConfiguration.getProjectService(), repositoryName);
            // wait for Black Duck to process the Project Version creation so that the components link will be available
            Thread.sleep(5000);
        } catch (IntegrationException e) {
            String message = "Could not get or create the Black Duck Project Version";
            logger.error(message + ": {}.", e.getMessage());
            logger.debug(e.getMessage(), e);
            throw new TaskInterruptedException(message, true);
        } catch (InterruptedException e) {
            String errorMessage = String.format("Waiting for the Project Version to be created was interrupted: %s", e.getMessage());
            logger.error(errorMessage);
            logger.debug(e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new TaskInterruptedException(errorMessage, true);
        }

        while (filteredAssets.hasResults()) {
            logger.info("Found some items from the DB");
            for (Asset asset : filteredAssets.getTypeList()) {
                AssetWrapper assetWrapper = AssetWrapper.createInspectionAssetWrapper(asset, inspectorConfiguration.getRepository(), commonRepositoryTaskHelper.getQueryManager());

                if (inspectorConfiguration.hasErrors()) {
                    commonRepositoryTaskHelper.failedConnection(assetWrapper, inspectorConfiguration.getExceptionMessage());
                    assetWrapper.updateAsset();
                } else {
                    processAsset(inspectorConfiguration.getProjectBomService(), inspectorConfiguration.getBlackDuckService(), projectVersionView, assetWrapper, inspectorConfiguration.getDependencyType());
                }
            }
            Query nextPage = commonRepositoryTaskHelper.createPagedQuery(filteredAssets.getLastName()).build();
            filteredAssets = commonRepositoryTaskHelper.retrievePagedAssets(inspectorConfiguration.getRepository(), nextPage);
        }
    }

    private void processAsset(ProjectBomService projectBomService, BlackDuckService blackDuckService, ProjectVersionView projectVersionView, AssetWrapper assetWrapper, DependencyType dependencyType) {
        String name = assetWrapper.getName();
        String version = assetWrapper.getVersion();

        DateTime lastModified = assetWrapper.getAssetLastUpdated();
        String fullPathName = assetWrapper.getFullPath();
        String fileName = null;
        try {
            fileName = assetWrapper.getFilename();
        } catch (IntegrationException e) {
            logger.debug(String.format("Skipping asset: %s. %s", name, e.getMessage()), e);
        }

        if (commonTaskFilters.isAssetTooOldForTask(lastModified, taskConfiguration)) {
            logger.debug("The asset is older than the task cutoff date: {}", name);
            return;
        } else if (!commonTaskFilters.doesAssetPathAndExtensionMatch(fullPathName, fileName, taskConfiguration)) {
            logger.debug("The asset path or extension does not match the task configuration: {}", name);
            return;
        }
        logger.debug("Inspecting item: {}, version: {}, path: {}", name, version, fullPathName);
        ExternalId externalId = dependencyGenerator.createExternalId(dependencyType, name, version, assetWrapper.getAsset().attributes());
        uploadAssetToBlackDuck(projectBomService, blackDuckService, projectVersionView, externalId, assetWrapper);
        assetWrapper.updateAsset();
    }

    private void uploadAssetToBlackDuck(ProjectBomService projectBomService, BlackDuckService blackDuckService, ProjectVersionView projectVersionView, ExternalId externalId, AssetWrapper assetWrapper) {
        String assetName = assetWrapper.getName();
        String assetVersion = assetWrapper.getVersion();
        try {
            Optional<String> componentURLOptional = projectBomService.addComponentToProjectVersion(externalId, projectVersionView);
            if (!componentURLOptional.isPresent()) {
                updateComponentNotFoundStatus(assetWrapper, String.format("The component %s:%s could not be found in Black Duck.", assetName, assetVersion));
            } else {
                ProjectVersionComponentView projectVersionComponentView = blackDuckService.getResponse(componentURLOptional.get(), ProjectVersionComponentView.class);
                BlackDuckServerConfig blackDuckServerConfig = commonRepositoryTaskHelper.getBlackDuckServerConfig();
                String blackDuckUrl = blackDuckServerConfig.getBlackDuckUrl().toString();
                inspectorMetaDataProcessor.processAssetComponent(projectVersionComponentView, blackDuckUrl, projectVersionView, assetWrapper, TaskStatus.SUCCESS);
            }
        } catch (IntegrationException e) {
            logger.error("Problem uploading asset {}:{} to Black Duck: {}.", assetWrapper.getName(), assetWrapper.getVersion(), e.getMessage());
            logger.debug(e.getMessage(), e);
            updateErrorStatus(assetWrapper, e.getMessage());
        }
    }

    private void updateErrorStatus(AssetWrapper assetWrapper, String error) {
        assetWrapper.removeAllBlackDuckData();
        assetWrapper.addFailureToBlackDuckPanel(error);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
        assetWrapper.updateAsset();
    }

    private void updateComponentNotFoundStatus(AssetWrapper assetWrapper, String componentMissingMessage) {
        assetWrapper.removeAllBlackDuckData();
        assetWrapper.addComponentNotFoundToBlackDuckPanel(componentMissingMessage);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
        assetWrapper.updateAsset();
    }
}
