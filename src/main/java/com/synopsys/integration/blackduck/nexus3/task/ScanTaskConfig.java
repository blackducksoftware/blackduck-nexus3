package com.synopsys.integration.blackduck.nexus3.task;

import org.joda.time.DateTime;

public class ScanTaskConfig {
    private String filePatterns;
    private DateTime oldArtifactCutoffDate;
    private boolean rescanFailures;
    private boolean alwaysScan;

    public ScanTaskConfig(final String filePatterns, final DateTime oldArtifactCutoffDate, final boolean rescanFailures, final boolean alwaysScan) {
        this.filePatterns = filePatterns;
        this.oldArtifactCutoffDate = oldArtifactCutoffDate;
        this.rescanFailures = rescanFailures;
        this.alwaysScan = alwaysScan;
    }

    public String getFilePatterns() {
        return filePatterns;
    }

    public void setFilePatterns(final String filePatterns) {
        this.filePatterns = filePatterns;
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
}

