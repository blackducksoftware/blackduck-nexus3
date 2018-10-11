package com.synopsys.integration.blackduck.nexus3.task;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.scan.ScanTaskDescriptor;
import com.synopsys.integration.blackduck.nexus3.task.scan.ScanTaskKeys;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanel;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.nexus3.util.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.util.BlackDuckConnection;
import com.synopsys.integration.blackduck.nexus3.util.DateTimeParser;
import com.synopsys.integration.blackduck.service.HubService;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.ScanStatusService;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;

@Named
@Singleton
public class CommonRepositoryTaskHelper {
    private final QueryManager queryManager;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DateTimeParser dateTimeParser;
    private final BlackDuckConnection blackDuckConnection;

    @Inject
    public CommonRepositoryTaskHelper(final QueryManager queryManager, final DateTimeParser dateTimeParser, final BlackDuckConnection blackDuckConnection) {
        this.queryManager = queryManager;
        this.dateTimeParser = dateTimeParser;
        this.blackDuckConnection = blackDuckConnection;
    }

    // TODO verify that the group repository will work accordingly here
    public boolean doesRepositoryApply(final Repository repository, final String repositoryField) {
        return repository.getName().equals(repositoryField);
    }

    public String getTaskMessage(final String taskName, final String repositoryField) {
        return String.format("Running BlackDuck %s for repository %s: ", taskName, repositoryField);
    }

    public HubServerConfig getHubServerConfig() {
        try {
            return blackDuckConnection.getHubServerConfig();
        } catch (final IntegrationException e) {
            throw new TaskInterruptedException("BlackDuck hub server config not set.", true);
        }
    }

    public HubServicesFactory getHubServicesFactory() {
        try {
            return blackDuckConnection.getHubServicesFactory();
        } catch (final IntegrationException e) {
            throw new TaskInterruptedException("BlackDuck hub server config not set.", true);
        }
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

    public String verifyUpload(final String name, final String version) {
        try {
            final HubServicesFactory hubServicesFactory = getHubServicesFactory();
            final ProjectService projectService = hubServicesFactory.createProjectService();
            final HubService hubService = hubServicesFactory.createHubService();
            final ProjectVersionWrapper projectVersionWrapper = projectService.getProjectVersion(name, version);
            final ProjectVersionView projectVersionView = projectVersionWrapper.getProjectVersionView();
            final ScanStatusService scanStatusService = hubServicesFactory.createScanStatusService(ScanStatusService.DEFAULT_TIMEOUT);
            scanStatusService.assertScansFinished(projectVersionView);
            return hubService.getFirstLink(projectVersionView, ProjectVersionView.COMPONENTS_LINK);
        } catch (final InterruptedException | IntegrationException e) {
            logger.error("Problem communicating with BlackDuck: {}", e.getMessage());
            return "Error retrieving URL.";
        }
    }

    public void finalStatus(final AssetWrapper assetWrapper, final String componentsUrl, final String uploadStatus) {
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.HUB_URL, componentsUrl);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_STATUS, uploadStatus);

        assetWrapper.updateAsset();
    }

    public CommonTaskConfig getTaskConfig(final TaskConfiguration taskConfiguration) {
        final String filePatterns = taskConfiguration.getString(ScanTaskKeys.FILE_PATTERNS.getParameterKey());
        final String artifactPath = taskConfiguration.getString(ScanTaskKeys.REPOSITORY_PATH.getParameterKey());
        final boolean rescanFailures = taskConfiguration.getBoolean(ScanTaskKeys.RESCAN_FAILURES.getParameterKey(), false);
        final boolean alwaysScan = taskConfiguration.getBoolean(ScanTaskKeys.ALWAYS_SCAN.getParameterKey(), false);
        final int limit = taskConfiguration.getInteger(ScanTaskKeys.PAGING_SIZE.getParameterKey(), ScanTaskDescriptor.DEFAULT_SCAN_PAGE_SIZE);

        final String artifactCutoff = taskConfiguration.getString(ScanTaskKeys.OLD_ARTIFACT_CUTOFF.getParameterKey());
        final DateTime oldArtifactCutoffDate = dateTimeParser.convertFromStringToDate(artifactCutoff);
        return new CommonTaskConfig(filePatterns, artifactPath, oldArtifactCutoffDate, rescanFailures, alwaysScan, limit);
    }

    public Query createFilteredQueryBuilder(final CommonTaskConfig commonTaskConfig, final Optional<String> lastNameUsed, final int limit) {
        final Query.Builder baseQueryBuilder = createPagedQuery(lastNameUsed, limit);

        //        final DateTime artifactCutoffDate = commonTaskConfig.getOldArtifactCutoffDate();
        //        if (artifactCutoffDate != null) {
        //            final String lastModifiedPath = "attributes.content.last_modified";
        //            baseQueryBuilder.and(lastModifiedPath + " > " + dateTimeParser.convertFromDateTimeToMillis(artifactCutoffDate));
        //        }
        //
        //        final String repositoryPathRegex = commonTaskConfig.getRepositoryPathRegex();
        //        if (StringUtils.isNotBlank(repositoryPathRegex)) {
        //            baseQueryBuilder.and("name MATCHES '" + repositoryPathRegex + "'");
        //        }

        final String statusSuccess = createSuccessWhereStatement(commonTaskConfig.isRescanFailures(), commonTaskConfig.isAlwaysScan());
        baseQueryBuilder.and(statusSuccess);

        final List<String> extensions = Arrays.stream(commonTaskConfig.getExtensionPatterns().split(","))
                                            .map(String::trim)
                                            .collect(Collectors.toList());
        final String extensionsCheck = createExtensionsWhereStatement(extensions);
        baseQueryBuilder.and(extensionsCheck);

        return baseQueryBuilder.build();
    }

    public String createSuccessWhereStatement(final boolean checkFailures, final boolean checkSuccessAndPending) {
        final String statusPath = getBlackduckPanelPath(AssetPanelLabel.TASK_STATUS);
        final StringBuilder extensionsWhereBuilder = new StringBuilder();
        extensionsWhereBuilder.append("(");
        extensionsWhereBuilder.append(statusPath + " IS NULL");
        if (checkSuccessAndPending) {
            extensionsWhereBuilder.append(" OR ");
            extensionsWhereBuilder.append(statusPath);
            extensionsWhereBuilder.append(" = '");
            extensionsWhereBuilder.append(TaskStatus.SUCCESS.name());
            extensionsWhereBuilder.append("'");
            extensionsWhereBuilder.append(" OR ");
            extensionsWhereBuilder.append(statusPath);
            extensionsWhereBuilder.append(" = '");
            extensionsWhereBuilder.append(TaskStatus.PENDING.name());
            extensionsWhereBuilder.append("'");
        }
        if (checkFailures) {
            extensionsWhereBuilder.append(" OR ");
            extensionsWhereBuilder.append(statusPath);
            extensionsWhereBuilder.append(" = '");
            extensionsWhereBuilder.append(TaskStatus.FAILURE.name());
            extensionsWhereBuilder.append("'");
        }
        extensionsWhereBuilder.append(")");
        return extensionsWhereBuilder.toString();
    }

    public String createExtensionsWhereStatement(final List<String> extensions) {
        final StringBuilder extensionsWhereBuilder = new StringBuilder();
        extensionsWhereBuilder.append("(");
        for (int extCount = 0; extCount < extensions.size(); extCount++) {
            final String extension = extensions.get(extCount);
            if (extCount == 0) {
                extensionsWhereBuilder.append("name LIKE '%");
                extensionsWhereBuilder.append(extension);
                extensionsWhereBuilder.append("'");
            } else {
                extensionsWhereBuilder.append(" OR name LIKE '%");
                extensionsWhereBuilder.append(extension);
                extensionsWhereBuilder.append("'");
            }
        }
        extensionsWhereBuilder.append(")");
        return extensionsWhereBuilder.toString();
    }

}
