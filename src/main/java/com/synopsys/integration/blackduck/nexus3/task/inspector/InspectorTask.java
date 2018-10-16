package com.synopsys.integration.blackduck.nexus3.task.inspector;

import java.io.File;
import java.io.IOException;
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
import com.synopsys.integration.blackduck.api.generated.view.VersionBomPolicyStatusView;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.CommonTaskConfig;
import com.synopsys.integration.blackduck.nexus3.task.CommonTaskKeys;
import com.synopsys.integration.blackduck.nexus3.task.TaskFilter;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.task.inspector.dependency.DependencyGenerator;
import com.synopsys.integration.blackduck.nexus3.task.inspector.dependency.DependencyType;
import com.synopsys.integration.blackduck.nexus3.task.metadata.MetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.nexus3.util.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.util.DateTimeParser;
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
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final QueryManager queryManager;
    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final DateTimeParser dateTimeParser;
    private final TaskFilter taskFilter;
    private final DependencyGenerator dependencyGenerator;
    private final MetaDataProcessor metaDataProcessor;

    @Inject
    public InspectorTask(final QueryManager queryManager, final CommonRepositoryTaskHelper commonRepositoryTaskHelper, final DateTimeParser dateTimeParser, final TaskFilter taskFilter, final DependencyGenerator dependencyGenerator,
        final MetaDataProcessor metaDataProcessor) {
        this.queryManager = queryManager;
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.dateTimeParser = dateTimeParser;
        this.taskFilter = taskFilter;
        this.dependencyGenerator = dependencyGenerator;
        this.metaDataProcessor = metaDataProcessor;
    }

    // TODO add vulnerabilities to inspected assets
    @Override
    protected void execute(final Repository repository) {
        final Optional<DependencyType> dependencyType = dependencyGenerator.findDependency(repository.getFormat());
        if (!dependencyType.isPresent()) {
            throw new TaskInterruptedException("Task being run on unsupported repository", true);
        }

        final CommonTaskConfig commonTaskConfig = commonRepositoryTaskHelper.getTaskConfig(getConfiguration());

        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
        final MutableDependencyGraph mutableDependencyGraph = simpleBdioFactory.createMutableDependencyGraph();

        final Query pagedQuery = commonRepositoryTaskHelper.createFilteredQueryBuilder(commonTaskConfig, Optional.empty());
        PagedResult<Asset> filteredAssets = commonRepositoryTaskHelper.pagedAssets(repository, pagedQuery);
        final boolean resultsFound = filteredAssets.hasResults();
        while (filteredAssets.hasResults()) {
            logger.info("Found some items from the DB");
            for (final Asset asset : filteredAssets.getTypeList()) {
                final AssetWrapper assetWrapper = new AssetWrapper(asset, repository, queryManager);
                final String name = assetWrapper.getName();
                final String version = assetWrapper.getVersion();

                if (commonRepositoryTaskHelper.skipAssetProcessing(assetWrapper, commonTaskConfig)) {
                    logger.debug("Binary file did not meet requirements for scan: {}", name);
                    continue;
                }

                final Dependency dependency = dependencyGenerator.createDependency(dependencyType.get(), name, version, asset.attributes());
                logger.debug("Created new dependency: {}", dependency);
                mutableDependencyGraph.addChildToRoot(dependency);

                assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_STATUS, TaskStatus.PENDING.name());
                assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
                assetWrapper.updateAsset();
            }

            final Query nextPage = commonRepositoryTaskHelper.createFilteredQueryBuilder(commonTaskConfig, filteredAssets.getLastName());
            filteredAssets = commonRepositoryTaskHelper.pagedAssets(repository, nextPage);
        }

        if (resultsFound) {
            logger.info("Creating hub project.");
            uploadToBlackDuck(repository, mutableDependencyGraph, simpleBdioFactory, dependencyType.get(), commonTaskConfig);
        } else {
            logger.warn("No assets found with set criteria.");
        }

    }

    private void uploadToBlackDuck(final Repository repository, final MutableDependencyGraph mutableDependencyGraph, final SimpleBdioFactory simpleBdioFactory, final DependencyType dependencyType, final CommonTaskConfig commonTaskConfig) {
        final Forge nexusForge = new Forge("/", "/", "nexus");
        final String projectName = repository.getName();
        final String projectVersion = "Nexus-3-Plugin";
        final ProjectVersionView projectVersionView;
        try {
            final HubServicesFactory hubServicesFactory = commonRepositoryTaskHelper.getHubServicesFactory();
            final ProjectService projectService = hubServicesFactory.createProjectService();
            final ProjectRequestBuilder projectRequestBuilder = new ProjectRequestBuilder();
            projectRequestBuilder.setProjectName(projectName);
            projectRequestBuilder.setVersionName(projectVersion);
            final ProjectVersionWrapper projectVersionWrapper = projectService.getProjectVersionAndCreateIfNeeded(projectRequestBuilder.buildObject());
            projectVersionView = projectVersionWrapper.getProjectVersionView();
            final String codeLocationName = String.join("/", projectName, projectVersion, dependencyType.getRepositoryType());
            final ExternalId projectRoot = simpleBdioFactory.createNameVersionExternalId(nexusForge, projectName, projectVersion);
            final SimpleBdioDocument simpleBdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, projectName, projectVersion, projectRoot, mutableDependencyGraph);
            sendInspectorData(simpleBdioDocument, simpleBdioFactory);
        } catch (final IntegrationException e) {
            logger.debug("Issue communicating with BlackDuck: {}", e.getMessage());
            throw new TaskInterruptedException("Issue communicating with BlackDuck", true);
        } catch (final IOException e) {
            logger.error("Error writing to file: {}", e.getMessage());
            throw new TaskInterruptedException("Couldn't save inspection data", true);
        }

        final String uploadUrl = commonRepositoryTaskHelper.verifyUpload(projectVersionView);
        final TaskStatus status = uploadUrl.startsWith("http") ? TaskStatus.SUCCESS : TaskStatus.FAILURE;
        VersionBomPolicyStatusView policyStatusView = null;
        try {
            policyStatusView = metaDataProcessor.checkAssetPolicy(projectName, projectVersion);
        } catch (final IntegrationException e) {
            logger.error("There was an issue checking Policy: {}", e.getMessage());
        }

        updateStatus(commonTaskConfig, repository, uploadUrl, status, policyStatusView);
    }

    private void updateStatus(final CommonTaskConfig commonTaskConfig, final Repository repository, final String uploadUrl, final TaskStatus status, final VersionBomPolicyStatusView policyStatusView) {
        final Query pagedQuery = commonRepositoryTaskHelper.createFilteredQueryBuilder(commonTaskConfig, Optional.empty());
        PagedResult<Asset> filteredAssets = commonRepositoryTaskHelper.pagedAssets(repository, pagedQuery);
        while (filteredAssets.hasResults()) {
            for (final Asset asset : filteredAssets.getTypeList()) {
                final AssetWrapper assetWrapper = new AssetWrapper(asset, repository, queryManager);
                commonRepositoryTaskHelper.addFinalPanelElements(assetWrapper, uploadUrl, status.name());
                if (policyStatusView != null) {
                    metaDataProcessor.updateAssetPolicyData(policyStatusView, assetWrapper);
                }
            }

            final Query nextPage = commonRepositoryTaskHelper.createFilteredQueryBuilder(commonTaskConfig, filteredAssets.getLastName());
            filteredAssets = commonRepositoryTaskHelper.pagedAssets(repository, nextPage);
        }
    }

    private void sendInspectorData(final SimpleBdioDocument bdioDocument, final SimpleBdioFactory simpleBdioFactory) throws IntegrationException, IOException {
        final HubServicesFactory hubServicesFactory = commonRepositoryTaskHelper.getHubServicesFactory();
        final CodeLocationService codeLocationService = hubServicesFactory.createCodeLocationService();

        final IntegrationEscapeUtil integrationEscapeUtil = new IntegrationEscapeUtil();
        final File workingDirectory = new File(getConfiguration().getString(CommonTaskKeys.WORKING_DIRECTORY.getParameterKey()), "inspector");
        workingDirectory.mkdirs();
        final File bdioFile = new File(workingDirectory, integrationEscapeUtil.escapeForUri(bdioDocument.billOfMaterials.spdxName));
        bdioFile.delete();
        bdioFile.createNewFile();
        simpleBdioFactory.writeSimpleBdioDocumentToFile(bdioFile, bdioDocument);

        codeLocationService.importBomFile(bdioFile);
    }

    @Override
    protected boolean appliesTo(final Repository repository) {
        return commonRepositoryTaskHelper.doesRepositoryApply(repository, getRepositoryField());
    }

    @Override
    public String getMessage() {
        return commonRepositoryTaskHelper.getTaskMessage("Inspector", getRepositoryField());
    }

}
