package com.synopsys.integration.blackduck.nexus3.task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.api.generated.view.CodeLocationView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.view.ScanSummaryView;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanel;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.nexus3.util.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.util.BlackDuckConnection;
import com.synopsys.integration.blackduck.nexus3.util.DateTimeParser;
import com.synopsys.integration.blackduck.service.CodeLocationService;
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
    private final TaskFilter taskFilter;

    @Inject
    public CommonRepositoryTaskHelper(final QueryManager queryManager, final DateTimeParser dateTimeParser, final BlackDuckConnection blackDuckConnection, final TaskFilter taskFilter) {
        this.queryManager = queryManager;
        this.dateTimeParser = dateTimeParser;
        this.blackDuckConnection = blackDuckConnection;
        this.taskFilter = taskFilter;
    }

    // TODO verify that the group repository will work accordingly here
    public boolean doesRepositoryApply(final Repository repository, final String repositoryField) {
        return repository.getName().equals(repositoryField);
    }

    public String getTaskMessage(final String taskName, final String repositoryField) {
        return String.format("Running BlackDuck %s for repository %s: ", taskName, repositoryField);
    }

    public QueryManager getQueryManager() {
        return queryManager;
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

    public boolean skipAssetProcessing(final AssetWrapper assetWrapper, final CommonTaskConfig commonTaskConfig) {
        final DateTime lastModified = assetWrapper.getComponentLastUpdated();
        final boolean doesRepositoryPathMatch = taskFilter.doesRepositoryPathMatch(assetWrapper.getName(), commonTaskConfig.getRepositoryPathRegex());
        final boolean isArtifactTooOld = taskFilter.isArtifactTooOld(commonTaskConfig.getOldArtifactCutoffDate(), lastModified);
        final boolean doesExtensionMatch = taskFilter.doesExtensionMatch(assetWrapper.getFilename(), commonTaskConfig.getExtensionPatterns());
        return isArtifactTooOld || !doesRepositoryPathMatch || !doesExtensionMatch;
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

    public String verifyUpload(final List<String> codeLocationNames, final String name, final String version) {
        try {
            final ProjectService projectService = getHubServicesFactory().createProjectService();
            final ProjectVersionWrapper projectVersionWrapper = projectService.getProjectVersion(name, version);
            return verifyUpload(codeLocationNames, projectVersionWrapper.getProjectVersionView());
        } catch (final IntegrationException e) {
            logger.error("Problem communicating with BlackDuck: {}", e.getMessage());
            return "Error retrieving URL: " + e.getMessage();
        }
    }

    public String verifyUpload(final List<String> codeLocationNames, final ProjectVersionView projectVersionView) {
        logger.debug("Checking that project exists in BlackDuck.");
        try {
            final CodeLocationService codeLocationService = getHubServicesFactory().createCodeLocationService();
            final HubService hubService = getHubServicesFactory().createHubService();

            final List<CodeLocationView> allCodeLocations = new ArrayList<>();
            for (final String codeLocationName : codeLocationNames) {
                final CodeLocationView codeLocationView = codeLocationService.getCodeLocationByName(codeLocationName);
                allCodeLocations.add(codeLocationView);
            }
            final List<ScanSummaryView> scanSummaryViews = new ArrayList<>();
            for (final CodeLocationView codeLocationView : allCodeLocations) {
                final String scansLink = hubService.getFirstLinkSafely(codeLocationView, CodeLocationView.SCANS_LINK);
                if (StringUtils.isNotBlank(scansLink)) {
                    final List<ScanSummaryView> codeLocationScanSummaryViews = hubService.getResponses(scansLink, ScanSummaryView.class, true);
                    scanSummaryViews.addAll(codeLocationScanSummaryViews);
                }
            }

            final ScanStatusService scanStatusService = getHubServicesFactory().createScanStatusService(ScanStatusService.DEFAULT_TIMEOUT);
            scanStatusService.assertScansFinished(scanSummaryViews);

            return hubService.getFirstLink(projectVersionView, ProjectVersionView.COMPONENTS_LINK);
        } catch (final IntegrationException | InterruptedException e) {
            logger.error("Problem communicating with BlackDuck: {}", e.getMessage());
            return "Error retrieving URL: " + e.getMessage();
        }
    }

    public void addFinalPanelElements(final AssetWrapper assetWrapper, final String componentsUrl, final String uploadStatus) {
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.BLACKDUCK_URL, componentsUrl);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_STATUS, uploadStatus);

        assetWrapper.updateAsset();
    }

    public CommonTaskConfig getTaskConfig(final TaskConfiguration taskConfiguration) {
        final String workingDirecetoryName = taskConfiguration.getString(CommonTaskKeys.WORKING_DIRECTORY.getParameterKey());
        final File workingDirectory = new File(workingDirecetoryName);
        final String filePatterns = taskConfiguration.getString(CommonTaskKeys.FILE_PATTERNS.getParameterKey());
        final String artifactPath = taskConfiguration.getString(CommonTaskKeys.REPOSITORY_PATH.getParameterKey(), "");
        final boolean rescanFailures = taskConfiguration.getBoolean(CommonTaskKeys.REDO_FAILURES.getParameterKey(), true);
        final boolean alwaysScan = taskConfiguration.getBoolean(CommonTaskKeys.ALWAYS_CHECK.getParameterKey(), true);
        final int limit = taskConfiguration.getInteger(CommonTaskKeys.PAGING_SIZE.getParameterKey(), 100);

        final String artifactCutoff = taskConfiguration.getString(CommonTaskKeys.OLD_ARTIFACT_CUTOFF.getParameterKey());
        final DateTime oldArtifactCutoffDate = dateTimeParser.convertFromStringToDate(artifactCutoff);
        return new CommonTaskConfig(workingDirectory, filePatterns, artifactPath, oldArtifactCutoffDate, rescanFailures, alwaysScan, limit);
    }

    // TODO make query building easier for the tasks
    public Query createFilteredQueryBuilder(final CommonTaskConfig commonTaskConfig, final Optional<String> lastNameUsed) {
        final Query.Builder baseQueryBuilder = createPagedQuery(lastNameUsed, commonTaskConfig.getLimit());

        final DateTime artifactCutoffDate = commonTaskConfig.getOldArtifactCutoffDate();
        if (artifactCutoffDate != null) {
            // TODO verify that recently updated artifacts are grabbed successfully.
            final String lastModifiedPath = "attributes.content.last_modified";
            baseQueryBuilder.and(lastModifiedPath + " > " + dateTimeParser.convertFromDateTimeToMillis(artifactCutoffDate));
        }

        final String statusSuccess = createSuccessWhereStatement(commonTaskConfig.isRescanFailures(), commonTaskConfig.isAlwaysScan());
        baseQueryBuilder.and(statusSuccess);

        return baseQueryBuilder.build();
    }

    public String createSuccessWhereStatement(final boolean checkFailures, final boolean checkSuccessAndPending) {
        final String statusPath = getBlackduckPanelPath(AssetPanelLabel.TASK_STATUS);
        final String processedByPath = getBlackduckPanelPath(AssetPanelLabel.TASK_FINISHED_TIME);
        final String lastModifiedPath = "attributes.content.last_modified";
        final StringBuilder extensionsWhereBuilder = new StringBuilder();
        extensionsWhereBuilder.append("(");
        extensionsWhereBuilder.append(statusPath + " IS NULL");
        extensionsWhereBuilder.append(" OR ");
        extensionsWhereBuilder.append(processedByPath);
        extensionsWhereBuilder.append(" < ");
        extensionsWhereBuilder.append(lastModifiedPath);
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

    // FIXME name doesn't always have the extension at the end (i.e. nuget repo). Must find another way to query for extensions
    public String createExtensionsWhereStatement(final List<String> extensions) {
        //        final String blobNameHeader = "blob." + BlobStore.BLOB_NAME_HEADER;
        final String blobNameHeader = "name";
        final StringBuilder extensionsWhereBuilder = new StringBuilder();
        extensionsWhereBuilder.append("(");
        for (int extCount = 0; extCount < extensions.size(); extCount++) {
            final String extension = extensions.get(extCount);
            if (extCount == 0) {
                extensionsWhereBuilder.append(blobNameHeader);
                extensionsWhereBuilder.append(" LIKE '%");
                extensionsWhereBuilder.append(extension);
                extensionsWhereBuilder.append("'");
            } else {
                extensionsWhereBuilder.append(" OR ");
                extensionsWhereBuilder.append(blobNameHeader);
                extensionsWhereBuilder.append(" LIKE '%");
                extensionsWhereBuilder.append(extension);
                extensionsWhereBuilder.append("'");
            }
        }
        extensionsWhereBuilder.append(")");
        return extensionsWhereBuilder.toString();
    }

}
