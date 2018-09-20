/**
 * blackduck-nexus3
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
