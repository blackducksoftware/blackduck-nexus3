package com.synopsys.integration.blackduck.nexus3.task.common;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
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
import com.synopsys.integration.blackduck.nexus3.BlackDuckConnection;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanel;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.HubService;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.ScanStatusService;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;

@Named
@Singleton
public class CommonRepositoryTaskHelper {
    public static final String VERIFICATION_ERROR = "Error retrieving URL: ";
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
        return String.format("Running %s for repository %s: ", taskName, repositoryField);
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

    public String getRepositoryPath(final TaskConfiguration taskConfiguration) {
        return taskConfiguration.getString(CommonTaskKeys.REPOSITORY_PATH.getParameterKey());
    }

    public String getFileExtensionPatterns(final TaskConfiguration taskConfiguration) {
        return taskConfiguration.getString(CommonTaskKeys.FILE_PATTERNS.getParameterKey());
    }

    public DateTime getArtifactCutoffDateTime(final TaskConfiguration taskConfiguration) {
        final String artifactCutoffString = taskConfiguration.getString(CommonTaskKeys.OLD_ARTIFACT_CUTOFF.getParameterKey());
        return dateTimeParser.convertFromStringToDate(artifactCutoffString);
    }

    public int getPagingSizeLimit(final TaskConfiguration taskConfiguration) {
        return taskConfiguration.getInteger(CommonTaskKeys.PAGING_SIZE.getParameterKey(), CommonDescriptorHelper.DEFAULT_PAGE_SIZE);
    }

    public File getWorkingDirectory(final TaskConfiguration taskConfiguration) {
        final String directoryName = taskConfiguration.getString(CommonTaskKeys.WORKING_DIRECTORY.getParameterKey());
        return new File(directoryName);
    }

    public boolean skipAssetProcessing(final AssetWrapper assetWrapper, final TaskConfiguration taskConfiguration) {
        final DateTime lastModified = assetWrapper.getComponentLastUpdated();
        final boolean doesRepositoryPathMatch = doesRepositoryPathMatch(assetWrapper.getName(), getRepositoryPath(taskConfiguration));
        final boolean isArtifactTooOld = isArtifactTooOld(getArtifactCutoffDateTime(taskConfiguration), lastModified);
        final boolean doesExtensionMatch = doesExtensionMatch(assetWrapper.getFilename(), getFileExtensionPatterns(taskConfiguration));
        return isArtifactTooOld || !doesRepositoryPathMatch || !doesExtensionMatch;
    }

    public boolean doesExtensionMatch(final String filename, final String allowedExtensions) {
        final Set<String> extensions = Arrays.stream(allowedExtensions.split(",")).map(String::trim).collect(Collectors.toSet());
        for (final String extensionPattern : extensions) {
            if (FilenameUtils.wildcardMatch(filename, extensionPattern)) {
                return true;
            }
        }
        return false;
    }

    public boolean doesRepositoryPathMatch(final String artifactPath, final String regexPattern) {
        if (StringUtils.isBlank(regexPattern)) {
            return true;
        }
        return Pattern.matches(regexPattern, artifactPath);
    }

    public boolean isArtifactTooOld(final DateTime cutoffDate, final DateTime lastUpdated) {
        return lastUpdated.isBefore(cutoffDate.getMillis());
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

    public PagedResult<Asset> retrievePagedAssets(final Repository repository, final Query filteredQuery) {
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

    public ProjectVersionWrapper getProjectVersionWrapper(final String name, final String version) throws IntegrationException {
        final HubServicesFactory hubServicesFactory = getHubServicesFactory();
        final ProjectService projectService = hubServicesFactory.createProjectService();
        return projectService.getProjectVersion(name, version);
    }

    public String verifyUpload(final String name, final String version) {
        try {
            final HubServicesFactory hubServicesFactory = getHubServicesFactory();
            final ScanStatusService scanStatusService = hubServicesFactory.createScanStatusService(ScanStatusService.DEFAULT_TIMEOUT);
            scanStatusService.assertScansFinished(name, version);
            final ProjectService projectService = hubServicesFactory.createProjectService();
            final ProjectVersionWrapper projectVersionWrapper = projectService.getProjectVersion(name, version);
            return verifyUpload(projectVersionWrapper.getProjectVersionView());
        } catch (final InterruptedException | IntegrationException e) {
            logger.error("Problem communicating with BlackDuck: {}", e.getMessage());
            return VERIFICATION_ERROR + e.getMessage();
        }
    }

    // FIXME VerifyUpload methods will need to be reworked for working version of hub-common
    public String verifyUpload(final ProjectVersionView projectVersionView) {
        logger.debug("Checking that project exists in BlackDuck.");
        try {
            final HubService hubService = getHubServicesFactory().createHubService();
            return hubService.getFirstLink(projectVersionView, ProjectVersionView.COMPONENTS_LINK);
        } catch (final IntegrationException e) {
            logger.error("Problem communicating with BlackDuck: {}", e.getMessage());
            return VERIFICATION_ERROR + e.getMessage();
        }
    }

    // TODO make query building easier for the tasks
    public Query createFilteredQueryBuilder(final boolean rescanFailures, final boolean alwaysScan, final Optional<String> lastNameUsed, final int limit) {
        final Query.Builder baseQueryBuilder = createPagedQuery(lastNameUsed, limit);

        //        final DateTime artifactCutoffDate = commonTaskConfig.getOldArtifactCutoffDate();
        //        if (artifactCutoffDate != null) {
        //// TODO verify that recently updated artifacts are grabbed successfully.
        //            final String lastModifiedPath = "attributes.content.last_modified";
        //            baseQueryBuilder.and(lastModifiedPath + " > " + dateTimeParser.convertFromDateTimeToMillis(artifactCutoffDate));
        //        }

        final String statusSuccess = createSuccessWhereStatement(rescanFailures, alwaysScan);
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
        final String nameColumn = "name";
        final StringBuilder extensionsWhereBuilder = new StringBuilder();
        extensionsWhereBuilder.append("(");
        for (int extCount = 0; extCount < extensions.size(); extCount++) {
            final String extension = extensions.get(extCount);
            if (extCount == 0) {
                extensionsWhereBuilder.append(nameColumn);
                extensionsWhereBuilder.append(" LIKE '%");
                extensionsWhereBuilder.append(extension);
                extensionsWhereBuilder.append("'");
            } else {
                extensionsWhereBuilder.append(" OR ");
                extensionsWhereBuilder.append(nameColumn);
                extensionsWhereBuilder.append(" LIKE '%");
                extensionsWhereBuilder.append(extension);
                extensionsWhereBuilder.append("'");
            }
        }
        extensionsWhereBuilder.append(")");
        return extensionsWhereBuilder.toString();
    }

}
