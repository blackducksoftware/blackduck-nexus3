package com.synopsys.integration.blackduck.nexus3.task;

import org.joda.time.DateTime;

public class CommonTaskConfig {
    private final String extensionPatterns;
    private final String repositoryPathRegex;
    private final DateTime oldArtifactCutoffDate;
    private final boolean rescanFailures;
    private final boolean alwaysScan;
    private final int limit;

    public CommonTaskConfig(final String extensionPatterns, final String repositoryPathRegex, final DateTime oldArtifactCutoffDate, final boolean rescanFailures, final boolean alwaysScan, final int limit) {
        this.extensionPatterns = extensionPatterns;
        this.repositoryPathRegex = repositoryPathRegex;
        this.oldArtifactCutoffDate = oldArtifactCutoffDate;
        this.rescanFailures = rescanFailures;
        this.alwaysScan = alwaysScan;
        this.limit = limit;
    }

    public String getExtensionPatterns() {
        return extensionPatterns;
    }

    public String getRepositoryPathRegex() {
        return repositoryPathRegex;
    }

    public DateTime getOldArtifactCutoffDate() {
        return oldArtifactCutoffDate;
    }

    public boolean isRescanFailures() {
        return rescanFailures;
    }

    public boolean isAlwaysScan() {
        return alwaysScan;
    }

    public int getLimit() {
        return limit;
    }
}

