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
package com.synopsys.integration.blackduck.nexus3.task.model;

public enum ScanTaskField {
    DISTRIBUTION("blackduck.hub.project.version.distribution"),
    FILE_PATTERNS("blackduck.hub.nexus.file.pattern.match.wildcards"),
    PHASE("blackduck.hub.project.version.phase"),
    REPOSITORY_FIELD_ID("repositoryId"),
    REPOSITORY_PATH_FIELD_ID("repositoryPath"),
    WORKING_DIRECTORY("blackduck.hub.nexus.working.directory"),
    OLD_ARTIFACT_CUTOFF("blackduck.hub.nexus.artifact.cutoff"),
    RESCAN_FAILURES("blackduck.hub.nexus.rescan.failures"),
    ALWAYS_SCAN("blackduck.hub.nexus.rescan.always"),
    PHONE_HOME("blackduck.hub.nexus.phonehome"),
    REPOSITORY("selectedRepository");

    private final String parameterKey;

    private ScanTaskField(final String parameterKey) {
        this.parameterKey = parameterKey;
    }

    public String getParameterKey() {
        return parameterKey;
    }

}
