package com.synopsys.integration.blackduck.nexus3.task.inspector;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.TaskFilter;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.task.inspector.dependency.DependencyGenerator;
import com.synopsys.integration.blackduck.nexus3.task.inspector.dependency.DependencyType;
import com.synopsys.integration.blackduck.nexus3.task.scan.ScanTaskConfig;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.nexus3.util.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.util.DateTimeParser;
import com.synopsys.integration.blackduck.service.CodeLocationService;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.SimpleBdioFactory;
import com.synopsys.integration.hub.bdio.graph.MutableDependencyGraph;
import com.synopsys.integration.hub.bdio.model.Forge;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.hub.bdio.model.dependency.Dependency;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalId;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.IntegrationEscapeUtil;

@Named
public class InspectorTask extends RepositoryTaskSupport {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final QueryManager queryManager;
    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final DateTimeParser dateTimeParser;
    private final TaskFilter taskFilter;
    private final DependencyGenerator dependencyGenerator;

    @Inject
    public InspectorTask(final QueryManager queryManager, final CommonRepositoryTaskHelper commonRepositoryTaskHelper, final DateTimeParser dateTimeParser, final TaskFilter taskFilter, final DependencyGenerator dependencyGenerator) {
        this.queryManager = queryManager;
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.dateTimeParser = dateTimeParser;
        this.taskFilter = taskFilter;
        this.dependencyGenerator = dependencyGenerator;
    }

    @Override
    protected void execute(final Repository repository) {
        final Optional<DependencyType> dependencyType = dependencyGenerator.findDependency(repository.getFormat());
        if (!dependencyType.isPresent()) {
            throw new TaskInterruptedException("Task being run on unsupported repository", true);
        }

        final HubServerConfig hubServerConfig = commonRepositoryTaskHelper.getHubServerConfig();
        final ScanTaskConfig scanTaskConfig = commonRepositoryTaskHelper.getTaskConfig(getConfiguration());

        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
        final MutableDependencyGraph mutableDependencyGraph = simpleBdioFactory.createMutableDependencyGraph();

        final Query pagedQuery = createPagedQuery(Optional.empty(), scanTaskConfig.getLimit());
        PagedResult<Asset> filteredAssets = commonRepositoryTaskHelper.pagedAssets(repository, pagedQuery);
        while (filteredAssets.hasResults()) {
            for (final Asset asset : filteredAssets.getTypeList()) {
                final AssetWrapper assetWrapper = new AssetWrapper(asset, repository, queryManager);
                final String name = assetWrapper.getName();
                final String version = assetWrapper.getVersion();

                final Dependency dependency = dependencyGenerator.createDependency(dependencyType.get(), name, version, asset.attributes());
                mutableDependencyGraph.addChildToRoot(dependency);
            }

            final Query nextPage = createPagedQuery(filteredAssets.getLastName(), scanTaskConfig.getLimit());
            filteredAssets = commonRepositoryTaskHelper.pagedAssets(repository, nextPage);
        }

        final Forge nexusForge = new Forge("/", "/", "nexus");
        final String projectName = repository.getName();
        final String projectVersion = "Nexus-3-Plugin";
        final String codeLocationName = String.join("/", projectName, projectVersion, dependencyType.get().getRepositoryType());
        final ExternalId projectRoot = simpleBdioFactory.createNameVersionExternalId(nexusForge, projectName, projectVersion);
        final SimpleBdioDocument simpleBdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, projectName, projectVersion, projectRoot, mutableDependencyGraph);

        try {
            sendInspectorData(hubServerConfig, simpleBdioDocument);
        } catch (final IntegrationException e) {
            logger.debug("Issue communicating with BlackDuck: {}", e.getMessage());
            throw new TaskInterruptedException("Issue communicating with BlackDuck", true);
        }

        updateStatus(scanTaskConfig, hubServerConfig, repository);
    }

    private void updateStatus(final ScanTaskConfig scanTaskConfig, final HubServerConfig hubServerConfig, final Repository repository) {
        final Query pagedQuery = createPagedQuery(Optional.empty(), scanTaskConfig.getLimit());
        PagedResult<Asset> filteredAssets = commonRepositoryTaskHelper.pagedAssets(repository, pagedQuery);
        while (filteredAssets.hasResults()) {
            final Set<AssetWrapper> assetWrappers = new HashSet<>();
            for (final Asset asset : filteredAssets.getTypeList()) {
                final AssetWrapper assetWrapper = new AssetWrapper(asset, repository, queryManager);
                assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_STATUS, TaskStatus.PENDING.name());
                assetWrapper.updateAsset();

                assetWrappers.add(assetWrapper);
            }

            final Query nextPage = createPagedQuery(filteredAssets.getLastName(), scanTaskConfig.getLimit());
            filteredAssets = commonRepositoryTaskHelper.pagedAssets(repository, nextPage);

            commonRepositoryTaskHelper.verifyAndMarkUpload(assetWrappers, hubServerConfig);
        }
    }

    private void sendInspectorData(final HubServerConfig hubServerConfig, final SimpleBdioDocument bdioDocument) throws IntegrationException {
        final IntLogger intLogger = new Slf4jIntLogger(logger);
        final HubServicesFactory hubServicesFactory = new HubServicesFactory(HubServicesFactory.createDefaultGson(), HubServicesFactory.createDefaultJsonParser(), hubServerConfig.createRestConnection(intLogger), intLogger);
        final CodeLocationService codeLocationService = hubServicesFactory.createCodeLocationService();

        final IntegrationEscapeUtil integrationEscapeUtil = new IntegrationEscapeUtil();
        final File bdioFile = new File("/inspector/" + integrationEscapeUtil.escapeForUri(bdioDocument.billOfMaterials.spdxName));

        codeLocationService.importBomFile(bdioFile);
    }

    private Query createPagedQuery(final Optional<String> lastNameUsed, final int limit) {
        return commonRepositoryTaskHelper.createPagedQuery(lastNameUsed, limit).build();
    }

    @Override
    protected boolean appliesTo(final Repository repository) {
        return commonRepositoryTaskHelper.doesRepositoryApply(repository, getRepositoryField()) && commonRepositoryTaskHelper.isProxyRepository(repository);
    }

    @Override
    public String getMessage() {
        return commonRepositoryTaskHelper.getTaskMessage("Inspector", getRepositoryField());
    }

}
