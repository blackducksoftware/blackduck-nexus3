package com.synopsys.integration.blackduck.nexus3.task.common;

import java.util.Arrays;
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
import org.sonatype.nexus.scheduling.TaskConfiguration;

import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;

@Named
@Singleton
public class CommonTaskFilters {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final DateTimeParser dateTimeParser;

    @Inject
    public CommonTaskFilters(final CommonRepositoryTaskHelper commonRepositoryTaskHelper, final DateTimeParser dateTimeParser) {
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.dateTimeParser = dateTimeParser;
    }

    public boolean skipAssetProcessing(final AssetWrapper assetWrapper, final TaskConfiguration taskConfiguration) {
        final DateTime lastModified = assetWrapper.getAssetLastUpdated();
        final String fullPathName = assetWrapper.getAsset().name();
        final String repositoryRegexPath = commonRepositoryTaskHelper.getRepositoryPath(taskConfiguration);
        final String fileExtensionPatterns = commonRepositoryTaskHelper.getFileExtensionPatterns(taskConfiguration);
        final DateTime assetCutoffDate = commonRepositoryTaskHelper.getAssetCutoffDateTime(taskConfiguration);
        final boolean doesRepositoryPathMatch = doesRepositoryPathMatch(fullPathName, repositoryRegexPath);
        final boolean isAssetTooOld = isAssetTooOld(assetCutoffDate, lastModified);
        final boolean doesExtensionMatch = doesExtensionMatch(assetWrapper.getFilename(), fileExtensionPatterns);

        logger.info("Checking if processing of {} should be skipped", fullPathName);
        logger.debug("Is asset to old, {}", isAssetTooOld);
        logger.debug("Does repository match, {}", doesRepositoryPathMatch);
        logger.debug("Does extension match, {}", doesExtensionMatch);
        return isAssetTooOld || !doesRepositoryPathMatch || !doesExtensionMatch;
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

    public boolean doesRepositoryPathMatch(final String assetPath, final String regexPattern) {
        if (StringUtils.isBlank(regexPattern)) {
            return true;
        }
        logger.debug("Artifact Path {} being checked against {}", assetPath, regexPattern);
        return Pattern.matches(regexPattern, assetPath);
    }

    public boolean isAssetTooOld(final DateTime cutoffDate, final DateTime lastUpdated) {
        return lastUpdated.isBefore(cutoffDate.getMillis());
    }

    public boolean hasAssetBeenModified(final AssetWrapper assetWrapper) {
        final DateTime lastModified = assetWrapper.getAssetLastUpdated();
        final String lastProcessedString = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME);
        final DateTime lastProcessed = dateTimeParser.convertFromStringToDate(lastProcessedString);
        final boolean neverProcessed = lastProcessed == null;
        logger.debug("Last modified: {}", lastModified);
        logger.debug("Last processed: {}", lastProcessed);
        return neverProcessed || lastModified.isAfter(lastProcessed);
    }
}