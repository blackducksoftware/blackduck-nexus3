package com.synopsys.integration.blackduck.nexus3.task.common;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;

@Named
@Singleton
public class CommonTaskFilters {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DateTimeParser dateTimeParser;
    private final Type hostedType;
    private final Type proxyType;
    private final Type groupType;

    @Inject
    public CommonTaskFilters(DateTimeParser dateTimeParser, @Named(HostedType.NAME) Type hostedType, @Named(ProxyType.NAME) Type proxyType,
        @Named(GroupType.NAME) Type groupType) {
        this.dateTimeParser = dateTimeParser;

        this.hostedType = hostedType;
        this.proxyType = proxyType;
        this.groupType = groupType;
    }

    public boolean isProxyRepository(Type repositoryType) {
        return proxyType.equals(repositoryType);
    }

    public boolean isHostedRepository(Type repositoryType) {
        return hostedType.equals(repositoryType);
    }

    public boolean isGroupRepository(Type repositoryType) {
        return groupType.equals(repositoryType);
    }

    public List<Repository> findRelevantRepositories(Repository repository) {
        if (isGroupRepository(repository.getType())) {
            GroupFacet groupFacet = repository.facet(GroupFacet.class);
            return groupFacet.leafMembers();
        }

        return Arrays.asList(repository);
    }

    public String getRepositoryPath(TaskConfiguration taskConfiguration) {
        return taskConfiguration.getString(CommonTaskKeys.REPOSITORY_PATH.getParameterKey());
    }

    public String getFileExtensionPatterns(TaskConfiguration taskConfiguration) {
        return taskConfiguration.getString(CommonTaskKeys.FILE_PATTERNS.getParameterKey());
    }

    public DateTime getAssetCutoffDateTime(TaskConfiguration taskConfiguration) {
        String assetCutoffString = taskConfiguration.getString(CommonTaskKeys.OLD_ASSET_CUTOFF.getParameterKey());
        return dateTimeParser.convertFromStringToDate(assetCutoffString);
    }

    public boolean skipAssetProcessing(DateTime lastModified, String fullPathName, String fileName, TaskConfiguration taskConfiguration) {
        String repositoryRegexPath = getRepositoryPath(taskConfiguration);
        String fileExtensionPatterns = getFileExtensionPatterns(taskConfiguration);
        DateTime assetCutoffDate = getAssetCutoffDateTime(taskConfiguration);
        boolean doesRepositoryPathMatch = doesRepositoryPathMatch(fullPathName, repositoryRegexPath);
        boolean isAssetTooOld = isAssetTooOld(assetCutoffDate, lastModified);
        boolean doesExtensionMatch = doesExtensionMatch(fileName, fileExtensionPatterns);

        logger.debug("Checking if processing of {} should be skipped", fullPathName);
        logger.debug("Is asset to old, {}", isAssetTooOld);
        logger.debug("Does repository match, {}", doesRepositoryPathMatch);
        logger.debug("Does extension match, {}", doesExtensionMatch);
        return isAssetTooOld || !doesRepositoryPathMatch || !doesExtensionMatch;
    }

    public boolean doesExtensionMatch(String filename, String allowedExtensions) {
        Set<String> extensions = Arrays.stream(allowedExtensions.split(",")).map(String::trim).collect(Collectors.toSet());
        for (String extensionPattern : extensions) {
            if (FilenameUtils.wildcardMatch(filename, extensionPattern)) {
                return true;
            }
        }
        return false;
    }

    public boolean doesRepositoryPathMatch(String assetPath, String regexPattern) {
        if (StringUtils.isBlank(regexPattern)) {
            return true;
        }
        logger.debug("Artifact Path {} being checked against {}", assetPath, regexPattern);
        return Pattern.matches(regexPattern, assetPath);
    }

    public boolean doesRepositoryApply(Repository repository, String repositoryField) {
        return repository.getName().equals(repositoryField);
    }

    public boolean isAssetTooOld(DateTime cutoffDate, DateTime lastUpdated) {
        return lastUpdated.isBefore(cutoffDate.getMillis());
    }

    public boolean hasAssetBeenModified(AssetWrapper assetWrapper) {
        DateTime lastModified = assetWrapper.getAssetLastUpdated();
        String lastProcessedString = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME);
        DateTime lastProcessed = dateTimeParser.convertFromStringToDate(lastProcessedString);
        boolean neverProcessed = lastProcessed == null;
        logger.debug("Last modified: {}", lastModified);
        logger.debug("Last processed: {}", lastProcessed);
        return neverProcessed || lastModified.isAfter(lastProcessed);
    }
}
