package com.synopsys.integration.blackduck.nexus3.task;

import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.capability.HubCapabilityConfiguration;
import com.synopsys.integration.blackduck.nexus3.capability.HubCapabilityFinder;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanel;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.nexus3.util.AssetWrapper;
import com.synopsys.integration.blackduck.service.HubService;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.ScanStatusService;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

@Named
@Singleton
public class CommonRepositoryTaskHelper {
    private final HubCapabilityFinder hubCapabilityFinder;
    private final QueryManager queryManager;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    public CommonRepositoryTaskHelper(final HubCapabilityFinder hubCapabilityFinder, final QueryManager queryManager) {
        this.hubCapabilityFinder = hubCapabilityFinder;
        this.queryManager = queryManager;
    }

    // TODO verify that the group repository will work accordingly here
    public boolean doesRepositoryApply(final Repository repository, final String repositoryField) {
        final String repositoryName = repositoryField;
        return repository.getName().equals(repositoryName);
    }

    public String getTaskMessage(final String taskName, final String repositoryField) {
        return String.format("Running BlackDuck %s for repository %s: ", taskName, repositoryField);
    }

    public HubServerConfig getHubServerConfig() {
        final HubCapabilityConfiguration hubCapabilityConfiguration = hubCapabilityFinder.retrieveHubCapabilityConfiguration();
        if (hubCapabilityConfiguration == null) {
            throw new TaskInterruptedException("BlackDuck hub server config not set.", true);
        }
        return hubCapabilityConfiguration.createHubServerConfig();
    }

    public Query.Builder createPagedQuery(final Optional<String> lastNameUsed, final int limit) {
        final Query.Builder pagedQueryBuilder = Query.builder();
        pagedQueryBuilder.where("component").isNotNull();
        if (lastNameUsed.isPresent()) {
            pagedQueryBuilder.and("name > ").param(lastNameUsed.get());
        }

        pagedQueryBuilder.suffix(String.format("ORDER BY name LIMIT %d", limit));
        return pagedQueryBuilder;
    }

    public PagedResult<Asset> pagedAssets(final Repository repository, final Query filteredQuery) {
        logger.debug("Running where statement from asset table of: {}. With the parameters: {}. And suffix: {}", filteredQuery.getWhere(), filteredQuery.getParameters(), filteredQuery.getQuerySuffix());
        final Iterable<Asset> filteredAssets = queryManager.findAssetsInRepository(repository, filteredQuery);
        final Optional<Asset> lastReturnedAsset = StreamSupport.stream(filteredAssets.spliterator(), true).reduce((first, second) -> second);
        Optional<String> name = Optional.empty();
        if (lastReturnedAsset.isPresent()) {
            name = Optional.of(lastReturnedAsset.get().name());
        }
        return new PagedResult<>(filteredAssets, name);
    }

    public String getBlackduckPanelPath(final AssetPanelLabel assetPanelLabel) {
        final String dbXmlPath = "attributes." + AssetPanel.BLACKDUCK_CATEGORY + ".";
        return dbXmlPath + assetPanelLabel.getLabel();
    }

    public void verifyAndMarkUpload(final Set<AssetWrapper> assetWrappers, final HubServerConfig hubServerConfig) throws IntegrationException, InterruptedException {
        final IntLogger intLogger = new Slf4jIntLogger(logger);
        final HubServicesFactory hubServicesFactory = new HubServicesFactory(HubServicesFactory.createDefaultGson(), HubServicesFactory.createDefaultJsonParser(), hubServerConfig.createRestConnection(intLogger), intLogger);
        final ScanStatusService scanStatusService = hubServicesFactory.createScanStatusService(ScanStatusService.DEFAULT_TIMEOUT);
        final ProjectService projectService = hubServicesFactory.createProjectService();
        final HubService hubService = hubServicesFactory.createHubService();
        logger.debug("Created hub services");
        for (final AssetWrapper assetWrapper : assetWrappers) {
            final String name = assetWrapper.getName();
            final String version = assetWrapper.getVersion();

            final ProjectVersionWrapper projectVersionWrapper = projectService.getProjectVersion(name, version);
            final ProjectVersionView projectVersionView = projectVersionWrapper.getProjectVersionView();
            scanStatusService.assertScansFinished(projectVersionView);
            logger.info("Project version found on server");
            final String componentsUrl = hubService.getFirstLink(projectVersionView, ProjectVersionView.COMPONENTS_LINK);
            logger.debug("Adding componentUrl to asset panel: {}", componentsUrl);
            assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.HUB_URL, componentsUrl);
            assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_STATUS, TaskStatus.SUCCESS.name());

            assetWrapper.updateAsset();
        }

    }

}
