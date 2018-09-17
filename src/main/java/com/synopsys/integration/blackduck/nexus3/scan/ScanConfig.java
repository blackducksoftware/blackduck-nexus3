package com.synopsys.integration.blackduck.nexus3.scan;

public class ScanConfig {
    private int memoryMB;
    private boolean dryRun;
    private String installDirectory;
    private String outputDirectory;
    private String projectName;
    private String projectVersion;

    public ScanConfig(final int memoryMB, final boolean dryRun, final String installDirectory, final String outputDirectory, final String projectName, final String projectVersion) {
        this.memoryMB = memoryMB;
        this.dryRun = dryRun;
        this.installDirectory = installDirectory;
        this.outputDirectory = outputDirectory;
        this.projectName = projectName;
        this.projectVersion = projectVersion;
    }

    public int getMemoryMB() {
        return memoryMB;
    }

    public void setMemoryMB(final int memoryMB) {
        this.memoryMB = memoryMB;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(final boolean dryRun) {
        this.dryRun = dryRun;
    }

    public String getInstallDirectory() {
        return installDirectory;
    }

    public void setInstallDirectory(final String installDirectory) {
        this.installDirectory = installDirectory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(final String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(final String projectName) {
        this.projectName = projectName;
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    public void setProjectVersion(final String projectVersion) {
        this.projectVersion = projectVersion;
    }
}
