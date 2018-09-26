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
package com.synopsys.integration.blackduck.nexus3.scan.model;

import java.io.File;

public class HubScannerConfig {
    private int memoryMB;
    private boolean dryRun;
    private File installDirectory;
    private File outputDirectory;

    public HubScannerConfig(final int memoryMB, final boolean dryRun, final File installDirectory, final File outputDirectory) {
        this.memoryMB = memoryMB;
        this.dryRun = dryRun;
        this.installDirectory = installDirectory;
        this.outputDirectory = outputDirectory;
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

    public File getInstallDirectory() {
        return installDirectory;
    }

    public void setInstallDirectory(final File installDirectory) {
        this.installDirectory = installDirectory;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(final File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

}
