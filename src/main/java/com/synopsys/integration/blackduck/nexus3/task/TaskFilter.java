package com.synopsys.integration.blackduck.nexus3.task;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

@Named
@Singleton
public class TaskFilter {

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

}
