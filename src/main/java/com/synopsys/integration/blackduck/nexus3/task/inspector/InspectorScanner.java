package com.synopsys.integration.blackduck.nexus3.task.inspector;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.api.generated.response.ComponentsView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.exception.BlackDuckApiException;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonTaskFilters;
import com.synopsys.integration.blackduck.nexus3.task.inspector.dependency.DependencyGenerator;
import com.synopsys.integration.blackduck.nexus3.task.inspector.dependency.DependencyType;
import com.synopsys.integration.blackduck.nexus3.task.inspector.model.TemporaryOriginView;
import com.synopsys.integration.blackduck.nexus3.task.inspector.wait.ComponentLinkWaitJob;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.ComponentService;
import com.synopsys.integration.blackduck.service.ProjectBomService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.rest.RestConstants;
import com.synopsys.integration.rest.exception.IntegrationRestException;
import com.synopsys.integration.wait.WaitJob;

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
        BlackDuckServerConfig blackDuckServerConfig;
        try {
            blackDuckServerConfig = commonRepositoryTaskHelper.getBlackDuckServerConfig();
        } catch (IntegrationException e) {
            String message = "Could not get the Black Duck Capability.";
            logger.error(message + ": {}.", e.getMessage());
            logger.debug(e.getMessage(), e);
            throw new TaskInterruptedException(message, true);
        }
        String blackDuckUrl = blackDuckServerConfig.getBlackDuckUrl().toString();

        String repositoryName = inspectorConfiguration.getRepository().getName();
        logger.info("Checking repository for assets: {}", repositoryName);
        Query pagedQuery = commonRepositoryTaskHelper.createPagedQuery(Optional.empty()).build();
        PagedResult<Asset> filteredAssets = commonRepositoryTaskHelper.retrievePagedAssets(inspectorConfiguration.getRepository(), pagedQuery);
        ProjectVersionView projectVersionView;
        try {
            logger.debug("Creating Project Version in Black Duck: {}", repositoryName);
            projectVersionView = inspectorMetaDataProcessor.getOrCreateProjectVersion(inspectorConfiguration.getBlackDuckService(), inspectorConfiguration.getProjectService(), repositoryName);
            // wait for Black Duck to process the Project Version creation so that the components link will be available
            String projectVersionViewHref = projectVersionView.getHref().orElseThrow(() -> new IntegrationException("Could not get the Href for the Black Duck Project Version."));

            boolean projectVersionHasComponentsLink = projectVersionView.hasLink(ProjectVersionView.COMPONENTS_LINK);
            if (!projectVersionHasComponentsLink) {
                logger.info("Waiting for the components link to be available for the Black Duck Project Version: {}:{}", repositoryName, projectVersionView.getVersionName());
                ComponentLinkWaitJob componentLinkWaitJob = new ComponentLinkWaitJob(projectVersionViewHref, inspectorConfiguration.getBlackDuckService());
                Long startTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                WaitJob waitForNotificationToBeProcessed = WaitJob.create(new Slf4jIntLogger(logger), 600, startTime, 30, componentLinkWaitJob);
                boolean isComplete = waitForNotificationToBeProcessed.waitFor();
                if (!isComplete) {
                    throw new IntegrationException("Could not find the Components link for the Black Duck Project Version.");
                }
            }
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
            Map<String, AssetWrapper> originIdToAsset = new HashMap<>();
            Iterable<Asset> assetsTypeList = filteredAssets.getTypeList();
            int assetCount = IterableUtils.size(assetsTypeList);
            logger.info("Found {} assets to inspect.", assetCount);
            for (Asset asset : assetsTypeList) {
                AssetWrapper assetWrapper = AssetWrapper.createInspectionAssetWrapper(asset, inspectorConfiguration.getRepository(), commonRepositoryTaskHelper.getQueryManager());

                if (inspectorConfiguration.hasErrors()) {
                    commonRepositoryTaskHelper.failedConnection(assetWrapper, inspectorConfiguration.getExceptionMessage());
                    assetWrapper.updateAsset();
                } else {
                    processAsset(inspectorConfiguration.getProjectBomService(), inspectorConfiguration.getComponentService(), inspectorConfiguration.getBlackDuckService(), projectVersionView, assetWrapper,
                        inspectorConfiguration.getDependencyType(), originIdToAsset);
                }
            }

            try {
                inspectorMetaDataProcessor.updateRepositoryMetaData(inspectorConfiguration.getProjectBomService(), blackDuckUrl, projectVersionView, originIdToAsset);
            } catch (IntegrationException e) {
                logger.error("Problem updating the assets with the Black Duck information: {}.", e.getMessage());
                logger.debug(e.getMessage(), e);
                updateErrorStatus(originIdToAsset.values(), e.getMessage());
            }
            Query nextPage = commonRepositoryTaskHelper.createPagedQuery(filteredAssets.getLastName()).build();
            filteredAssets = commonRepositoryTaskHelper.retrievePagedAssets(inspectorConfiguration.getRepository(), nextPage);
        }
    }

    private void processAsset(ProjectBomService projectBomService, ComponentService componentService, BlackDuckService blackDuckService, ProjectVersionView projectVersionView,
        AssetWrapper assetWrapper, DependencyType dependencyType, Map<String, AssetWrapper> originIdToAsset) {
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
        addAssetToBlackDuckProjectVersion(projectBomService, componentService, blackDuckService, projectVersionView, externalId, assetWrapper, originIdToAsset);
        assetWrapper.updateAsset();
    }

    private void addAssetToBlackDuckProjectVersion(ProjectBomService projectBomService, ComponentService componentService, BlackDuckService blackDuckService, ProjectVersionView projectVersionView, ExternalId externalId,
        AssetWrapper assetWrapper, Map<String, AssetWrapper> originIdToAsset) {
        String assetName = assetWrapper.getName();
        String assetVersion = assetWrapper.getVersion();
        try {
            Optional<String> componentURLOptional = addComponentToBom(projectBomService, componentService, externalId, projectVersionView);
            if (!componentURLOptional.isPresent()) {
                inspectorMetaDataProcessor.updateComponentNotFoundStatus(assetWrapper, String.format("The component %s:%s could not be found in Black Duck.", assetName, assetVersion));
            } else {
                String componentURL = componentURLOptional.get();
                // the response should be com.synopsys.integration.blackduck.api.generated.view.OriginView but the API of OriginView is incorrect so Gson can not convert the response to this class
                TemporaryOriginView originView = blackDuckService.getResponse(componentURL, TemporaryOriginView.class);
                String originId = originView.getOriginId();
                assetWrapper.addPendingToBlackDuckPanel("Asset waiting to be uploaded to Black Duck.");
                assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.ASSET_ORIGIN_ID, originId);
                assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());

                logger.debug("Adding asset to map with originId as key: {}", originId);
                originIdToAsset.put(originId, assetWrapper);
            }
        } catch (IntegrationException e) {
            logger.error("Problem uploading asset {}:{} to Black Duck: {}.", assetWrapper.getName(), assetWrapper.getVersion(), e.getMessage());
            logger.debug(e.getMessage(), e);
            updateErrorStatus(assetWrapper, e.getMessage());
        }
    }

    private Optional<String> addComponentToBom(ProjectBomService projectBomService, ComponentService componentService, ExternalId externalId, ProjectVersionView projectVersionView) throws IntegrationException {
        try {
            return projectBomService.addComponentToProjectVersion(externalId, projectVersionView);
        } catch (BlackDuckApiException e) {
            IntegrationRestException integrationRestException = e.getOriginalIntegrationRestException();
            if (RestConstants.PRECON_FAILED_412 == integrationRestException.getHttpStatusCode()) {
                // component is already part of the BOM, there is no quick way to determine this ahead of time short of retrieving the whole BOM and searching through the components
                return Optional.ofNullable(getComponentVersionUrl(componentService, externalId));
            }
            throw e;
        }
    }

    private String getComponentVersionUrl(ComponentService componentService, ExternalId externalId) throws IntegrationException {
        Optional<ComponentsView> componentSearchResultView = componentService.getFirstOrEmptyResult(externalId);
        String componentVersionUrl = null;
        if (componentSearchResultView.isPresent()) {
            ComponentsView componentsView = componentSearchResultView.get();
            if (StringUtils.isNotBlank(componentsView.getVariant())) {
                componentVersionUrl = componentsView.getVariant();
            } else {
                componentVersionUrl = componentsView.getVersion();
            }
        }
        return componentVersionUrl;
    }

    private void updateErrorStatus(Collection<AssetWrapper> assetWrappers, String error) {
        for (AssetWrapper assetWrapper : assetWrappers) {
            updateErrorStatus(assetWrapper, error);
        }
    }

    private void updateErrorStatus(AssetWrapper assetWrapper, String error) {
        assetWrapper.removeAllBlackDuckData();
        assetWrapper.addFailureToBlackDuckPanel(error);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
        assetWrapper.updateAsset();
    }

}
