/*
 * Copyright (C) 2018 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.hub.nexus3.task;

public enum TaskField {
    DISTRIBUTION("blackduck.hub.project.version.distribution"),
    FILE_PATTERNS("blackduck.hub.nexus.file.pattern.match.wildcards"),
    PHASE("blackduck.hub.project.version.phase"),
    REPOSITORY_FIELD_ID("repositoryId"),
    REPOSITORY_PATH_FIELD_ID("repositoryPath"),
    WORKING_DIRECTORY("blackduck.hub.nexus.working.directory"),
    OLD_ARTIFACT_CUTOFF("blackduck.hub.nexus.artifact.cutoff"),
    RESCAN_FAILURES("blackduck.hub.nexus.rescan.failures"),
    ALWAYS_SCAN("blackduck.hub.nexus.rescan.always"),
    PHONE_HOME("blackduck.hub.nexus.phonehome");

    private String parameterKey;

    private TaskField(final String parameterKey) {
        this.parameterKey = parameterKey;
    }

    public String getParameterKey() {
        return this.parameterKey;
    }

}
