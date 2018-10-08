package com.synopsys.integration.blackduck.nexus3.task.scan;

import org.joda.time.DateTime;

public class ScanTaskConfig {
    private String extensionPatterns;
    private String repositoryPathRegex;
    private DateTime oldArtifactCutoffDate;
    private boolean rescanFailures;
    private boolean alwaysScan;
    private int limit;

    public ScanTaskConfig(final String extensionPatterns, final String repositoryPathRegex, final DateTime oldArtifactCutoffDate, final boolean rescanFailures, final boolean alwaysScan, final int limit) {
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

    public void setExtensionPatterns(final String extensionPatterns) {
        this.extensionPatterns = extensionPatterns;
    }

    public String getRepositoryPathRegex() {
        return repositoryPathRegex;
    }

    public void setRepositoryPathRegex(final String repositoryPathRegex) {
        this.repositoryPathRegex = repositoryPathRegex;
    }

    public DateTime getOldArtifactCutoffDate() {
        return oldArtifactCutoffDate;
    }

    public void setOldArtifactCutoffDate(final DateTime oldArtifactCutoffDate) {
        this.oldArtifactCutoffDate = oldArtifactCutoffDate;
    }

    public boolean isRescanFailures() {
        return rescanFailures;
    }

    public void setRescanFailures(final boolean rescanFailures) {
        this.rescanFailures = rescanFailures;
    }

    public boolean isAlwaysScan() {
        return alwaysScan;
    }

    public void setAlwaysScan(final boolean alwaysScan) {
        this.alwaysScan = alwaysScan;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(final int limit) {
        this.limit = limit;
    }
}

