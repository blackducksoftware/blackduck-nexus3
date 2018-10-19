package com.synopsys.integration.blackduck.nexus3.task.inspector;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.api.generated.component.RiskCountView;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.task.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.CommonTaskConfig;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.task.inspector.dependency.DependencyGenerator;
import com.synopsys.integration.blackduck.nexus3.task.inspector.dependency.DependencyType;
import com.synopsys.integration.blackduck.nexus3.task.metadata.MetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.task.metadata.VulnerabilityLevels;
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

    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final DateTimeParser dateTimeParser;
    private final DependencyGenerator dependencyGenerator;
    private final MetaDataProcessor metaDataProcessor;

    @Inject
    public InspectorTask(final CommonRepositoryTaskHelper commonRepositoryTaskHelper, final DateTimeParser dateTimeParser, final DependencyGenerator dependencyGenerator,
        final MetaDataProcessor metaDataProcessor) {
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.dateTimeParser = dateTimeParser;
        this.dependencyGenerator = dependencyGenerator;
        this.metaDataProcessor = metaDataProcessor;
    }

    @Override
    protected void execute(final Repository repository) {
        final Optional<DependencyType> dependencyType = dependencyGenerator.findDependency(repository.getFormat());
        if (!dependencyType.isPresent()) {
            throw new TaskInterruptedException("Task being run on unsupported repository", true);
        }

        final CommonTaskConfig commonTaskConfig = commonRepositoryTaskHelper.getTaskConfig(getConfiguration());

        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
        final MutableDependencyGraph mutableDependencyGraph = simpleBdioFactory.createMutableDependencyGraph();
        final Map<String, AssetWrapper> assetWrapperMap = new HashMap<>();

        final Query pagedQuery = commonRepositoryTaskHelper.createFilteredQueryBuilder(commonTaskConfig, Optional.empty());
        PagedResult<Asset> filteredAssets = commonRepositoryTaskHelper.pagedAssets(repository, pagedQuery);
        final boolean resultsFound = filteredAssets.hasResults();
        while (filteredAssets.hasResults()) {
            logger.info("Found some items from the DB");
            for (final Asset asset : filteredAssets.getTypeList()) {
                final AssetWrapper assetWrapper = new AssetWrapper(asset, repository, commonRepositoryTaskHelper.getQueryManager());
                final String name = assetWrapper.getName();
                final String version = assetWrapper.getVersion();

                if (commonRepositoryTaskHelper.skipAssetProcessing(assetWrapper, commonTaskConfig)) {
                    logger.debug("Binary file did not meet requirements for inspection: {}", name);
                    continue;
                }

                final Dependency dependency = dependencyGenerator.createDependency(dependencyType.get(), name, version, asset.attributes());
                logger.debug("Created new dependency: {}", dependency);
                mutableDependencyGraph.addChildToRoot(dependency);

                final String originId = dependency.externalId.createHubOriginId();
                assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_STATUS, TaskStatus.PENDING.name());
                assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.ASSET_ORIGIN_ID, originId);
                assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
                assetWrapper.updateAsset();
                logger.debug("Adding asset to map with originId as key: {}", originId);
                assetWrapperMap.put(originId, assetWrapper);
            }

            final Query nextPage = commonRepositoryTaskHelper.createFilteredQueryBuilder(commonTaskConfig, filteredAssets.getLastName());
            filteredAssets = commonRepositoryTaskHelper.pagedAssets(repository, nextPage);
        }

        if (resultsFound) {
            logger.info("Creating hub project.");
            uploadToBlackDuck(repository, commonTaskConfig.getWorkingDirectory(), mutableDependencyGraph, simpleBdioFactory, dependencyType.get(), assetWrapperMap);
        } else {
            logger.warn("No assets found with set criteria.");
        }

    }

    private void uploadToBlackDuck(final Repository repository, final File workingDirectory, final MutableDependencyGraph mutableDependencyGraph, final SimpleBdioFactory simpleBdioFactory, final DependencyType dependencyType,
        final Map<String, AssetWrapper> assetWrapperMap) {
        final Forge nexusForge = new Forge("/", "/", "nexus");
        final String projectName = repository.getName();
        final String projectVersion = "Nexus-3-Plugin";
        final ProjectVersionView projectVersionView;
        final String codeLocationName = String.join("/", projectName, projectVersion, dependencyType.getRepositoryType());
        try {
            logger.debug("Creating project in BlackDuck if needed: {}", projectName);
            final HubServicesFactory hubServicesFactory = commonRepositoryTaskHelper.getHubServicesFactory();
            final ProjectService projectService = hubServicesFactory.createProjectService();
            final ProjectRequestBuilder projectRequestBuilder = new ProjectRequestBuilder();
            projectRequestBuilder.setProjectName(projectName);
            projectRequestBuilder.setVersionName(projectVersion);
            final ProjectVersionWrapper projectVersionWrapper = projectService.getProjectVersionAndCreateIfNeeded(projectRequestBuilder.buildObject());
            projectVersionView = projectVersionWrapper.getProjectVersionView();
            final ExternalId projectRoot = simpleBdioFactory.createNameVersionExternalId(nexusForge, projectName, projectVersion);
            final SimpleBdioDocument simpleBdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, projectName, projectVersion, projectRoot, mutableDependencyGraph);
            sendInspectorData(simpleBdioDocument, simpleBdioFactory, workingDirectory);
        } catch (final IntegrationException e) {
            logger.debug("Issue communicating with BlackDuck: {}", e.getMessage());
            throw new TaskInterruptedException("Issue communicating with BlackDuck", true);
        } catch (final IOException e) {
            logger.error("Error writing to file: {}", e.getMessage());
            throw new TaskInterruptedException("Couldn't save inspection data", true);
        }

        final String uploadUrl = commonRepositoryTaskHelper.verifyUpload(Arrays.asList(codeLocationName), projectVersionView);
        final TaskStatus status = uploadUrl.startsWith("http") ? TaskStatus.SUCCESS : TaskStatus.FAILURE;
        updateStatus(projectVersionView, status, assetWrapperMap);
    }

    private void sendInspectorData(final SimpleBdioDocument bdioDocument, final SimpleBdioFactory simpleBdioFactory, final File workingDirectory) throws IntegrationException, IOException {
        final HubServicesFactory hubServicesFactory = commonRepositoryTaskHelper.getHubServicesFactory();
        final CodeLocationService codeLocationService = hubServicesFactory.createCodeLocationService();

        final IntegrationEscapeUtil integrationEscapeUtil = new IntegrationEscapeUtil();
        final File blackDuckWorkingDirectory = new File(workingDirectory, "inspector");
        blackDuckWorkingDirectory.mkdirs();
        final File bdioFile = new File(blackDuckWorkingDirectory, integrationEscapeUtil.escapeForUri(bdioDocument.billOfMaterials.spdxName));
        bdioFile.delete();
        bdioFile.createNewFile();
        logger.debug("Sending data to BlackDuck.");
        simpleBdioFactory.writeSimpleBdioDocumentToFile(bdioFile, bdioDocument);

        codeLocationService.importBomFile(bdioFile);
    }

    private void updateStatus(final ProjectVersionView projectVersionView, final TaskStatus status, final Map<String, AssetWrapper> assetWrapperMap) {
        List<VersionBomComponentView> versionBomComponentViews = Collections.emptyList();
        try {
            logger.debug("Retrieving all components from Project.");
            versionBomComponentViews = metaDataProcessor.checkAssetVulnerabilities(projectVersionView);
        } catch (final IntegrationException e) {
            logger.error("Problem retrieving components from Project: {}", e.getMessage());
        }

        for (final VersionBomComponentView versionBomComponentView : versionBomComponentViews) {
            final Set<String> externalIds = versionBomComponentView.origins.stream()
                                                .map(versionBomOriginView -> versionBomOriginView.externalId)
                                                .collect(Collectors.toSet());
            logger.debug("Found all externalIds ({}) for component: {}", externalIds, versionBomComponentView.componentName);
            final Optional<AssetWrapper> assetWrapperOptional = findAssetWrapper(assetWrapperMap, externalIds);
            if (assetWrapperOptional.isPresent()) {
                final AssetWrapper assetWrapper = assetWrapperOptional.get();

                final String componentUrl = versionBomComponentView.componentVersion;
                final PolicySummaryStatusType policyStatus = versionBomComponentView.policyStatus;

                logger.info("Found component and updating Asset: {}", assetWrapper.getName());
                assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_STATUS, status.name());
                assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.BLACKDUCK_URL, componentUrl);
                assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS, policyStatus.prettyPrint());
                addVulnerabilityStatus(assetWrapper, versionBomComponentView);
            }
        }
    }

    private void addVulnerabilityStatus(final AssetWrapper assetWrapper, final VersionBomComponentView versionBomComponentView) {
        final VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();
        final List<RiskCountView> riskCountViews = versionBomComponentView.securityRiskProfile.counts;
        logger.info("Counting vulnerabilities");
        metaDataProcessor.addAllAssetVulnerabilityCounts(riskCountViews, vulnerabilityLevels);
        metaDataProcessor.updateAssetVulnerabilityData(vulnerabilityLevels, assetWrapper);
    }

    private Optional<AssetWrapper> findAssetWrapper(final Map<String, AssetWrapper> assetWrapperMap, final Set<String> externalIds) {
        return externalIds.stream()
                   .filter(externalId -> assetWrapperMap.get(externalId) != null)
                   .map(externalId -> assetWrapperMap.get(externalId))
                   .findFirst();
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
