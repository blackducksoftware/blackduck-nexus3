package com.synopsys.integration.blackduck.nexus3.task.inspector;

import static com.synopsys.integration.blackduck.nexus3.task.inspector.InspectorTask.INSPECTOR_VERSION_NAME;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskConfiguration;
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
import com.synopsys.integration.blackduck.service.ComponentService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.util.IntegrationEscapeUtil;

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
        SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
        MutableDependencyGraph mutableDependencyGraph = simpleBdioFactory.createMutableDependencyGraph();
        Map<String, AssetWrapper> assetWrapperMap = new HashMap<>();

        String repoName = inspectorConfiguration.getRepository().getName();
        logger.info("Checking repository for assets: {}", repoName);
        Query pagedQuery = commonRepositoryTaskHelper.createPagedQuery(Optional.empty()).build();
        PagedResult<Asset> filteredAssets = commonRepositoryTaskHelper.retrievePagedAssets(inspectorConfiguration.getRepository(), pagedQuery);
        boolean uploadToBlackDuck = false;
        while (filteredAssets.hasResults()) {
            logger.info("Found some items from the DB");
            for (Asset asset : filteredAssets.getTypeList()) {
                AssetWrapper assetWrapper = AssetWrapper.createInspectionAssetWrapper(asset, inspectorConfiguration.getRepository(), commonRepositoryTaskHelper.getQueryManager());

                if (inspectorConfiguration.hasErrors()) {
                    commonRepositoryTaskHelper.failedConnection(assetWrapper, inspectorConfiguration.getExceptionMessage());
                    assetWrapper.updateAsset();
                } else {
                    boolean shouldProcessAsset = processAsset(inspectorConfiguration.getComponentService(), assetWrapper, inspectorConfiguration.getDependencyType(), mutableDependencyGraph, assetWrapperMap);
                    if (shouldProcessAsset) {
                        // Only set uploadToBlackDuck to true, if you set it to false you risk falsely reporting that there are no new assets
                        // I believe this can be improved upon... -BM
                        uploadToBlackDuck = true;
                    }
                }
            }

            Query nextPage = commonRepositoryTaskHelper.createPagedQuery(filteredAssets.getLastName()).build();
            filteredAssets = commonRepositoryTaskHelper.retrievePagedAssets(inspectorConfiguration.getRepository(), nextPage);
        }

        if (uploadToBlackDuck && !inspectorConfiguration.hasErrors()) {
            logger.info("Creating Black Duck project.");
            uploadToBlackDuck(repoName, mutableDependencyGraph, simpleBdioFactory, inspectorConfiguration.getDependencyType(), assetWrapperMap);
        } else {
            logger.warn("Won't upload to Black Duck as no items were processed.");
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
            logger.debug(String.format("Skipping asset: %s. %s", name, e.getMessage()), e);
        }

        if (commonTaskFilters.skipAssetProcessing(lastModified, fullPathName, fileName, taskConfiguration)) {
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
            logger.debug(e.getMessage(), e);
            assetWrapper.addFailureToBlackDuckPanel(String.format("Something went wrong communicating with Black Duck: %s", e.getMessage()));
        }
        assetWrapper.updateAsset();
        return true;
    }

    private void uploadToBlackDuck(String repositoryName, MutableDependencyGraph mutableDependencyGraph, SimpleBdioFactory simpleBdioFactory, DependencyType dependencyType,
        Map<String, AssetWrapper> assetWrapperMap) {
        Forge nexusForge = new Forge("/", "nexus");
        ProjectVersionView projectVersionView;
        String codeLocationName = String.join("/", INSPECTOR_VERSION_NAME, repositoryName, dependencyType.getRepositoryType());
        try {
            BlackDuckServerConfig blackDuckServerConfig = commonRepositoryTaskHelper.getBlackDuckServerConfig();
            String blackDuckUrl = blackDuckServerConfig.getBlackDuckUrl().toString();
            int timeOutInSeconds = blackDuckServerConfig.getTimeout();

            logger.debug("Creating project in Black Duck if needed: {}", repositoryName);
            projectVersionView = inspectorMetaDataProcessor.getOrCreateProjectVersion(inspectorConfiguration.getBlackDuckService(), inspectorConfiguration.getProjectService(), repositoryName);

            ExternalId projectRoot = simpleBdioFactory.createNameVersionExternalId(nexusForge, repositoryName, INSPECTOR_VERSION_NAME);
            SimpleBdioDocument simpleBdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, repositoryName, INSPECTOR_VERSION_NAME, projectRoot, mutableDependencyGraph);

            CodeLocationCreationData<UploadBatchOutput> uploadData = sendInspectorData(inspectorConfiguration.getBdioUploadService(), simpleBdioDocument, simpleBdioFactory, codeLocationName);

            Set<String> successfulCodeLocationNames = uploadData.getOutput().getSuccessfulCodeLocationNames();
            CodeLocationWaitResult.Status status = CodeLocationWaitResult.Status.PARTIAL;
            if (successfulCodeLocationNames.contains(codeLocationName)) {
                CodeLocationWaitResult codeLocationWaitResult = inspectorConfiguration.getCodeLocationCreationService().waitForCodeLocations(uploadData.getNotificationTaskRange(), successfulCodeLocationNames,
                    successfulCodeLocationNames.size(), timeOutInSeconds * 5L);
                status = codeLocationWaitResult.getStatus();
            }
            if (CodeLocationWaitResult.Status.COMPLETE == status) {
                inspectorMetaDataProcessor.updateRepositoryMetaData(inspectorConfiguration.getBlackDuckService(), blackDuckUrl, projectVersionView, assetWrapperMap, TaskStatus.SUCCESS);
            } else {
                inspectorMetaDataProcessor.updateRepositoryMetaData(inspectorConfiguration.getBlackDuckService(), blackDuckUrl, projectVersionView, assetWrapperMap, TaskStatus.FAILURE);
            }
        } catch (BlackDuckApiException e) {
            logger.error("Problem communicating with Black Duck: {}", e.getMessage());
            logger.debug(e.getMessage(), e);
            updateErrorStatus(assetWrapperMap.values(), e.getMessage());
            throw new TaskInterruptedException("Problem communicating with Black Duck", true);
        } catch (IntegrationException e) {
            logger.error(String.format("Issue communicating with Black Duck: %s", e.getMessage()), e);
            updateErrorStatus(assetWrapperMap.values(), e.getMessage());
            throw new TaskInterruptedException("Issue communicating with Black Duck", true);
        } catch (IOException e) {
            logger.error("Error writing to file: {}", e.getMessage());
            logger.debug(e.getMessage(), e);
            updateErrorStatus(assetWrapperMap.values(), e.getMessage());
            throw new TaskInterruptedException("Couldn't save inspection data", true);
        } catch (InterruptedException e) {
            logger.error("Waiting for the results from Black Duck was interrupted: {}", e.getMessage());
            logger.debug(e.getMessage(), e);
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
        File workingDirectory = commonRepositoryTaskHelper.getWorkingDirectory(taskConfiguration);
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
}
