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
package com.synopsys.integration.blackduck.nexus3.task.common;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

public class CommonDescriptorHelper {
    public static final String DEFAULT_WORKING_DIRECTORY = "../sonatype-work";
    public static final String DEFAULT_ARTIFACT_CUTOFF = "2016-01-01T00:00:00.000";

    public static final String LABEL_REPOSITORY = "Black Duck - Repository";
    public static final String LABEL_REPOSITORY_PATH = "Black Duck - Repository Path";
    public static final String LABEL_FILE_PATTERN_MATCHES = "Black Duck - File Pattern Matches";
    public static final String LABEL_WORKING_DIRECTORY = "Black Duck - Working Directory";
    public static final String LABEL_ASSET_CUTOFF = "Black Duck - Asset Cutoff Date";

    public static final String DESCRIPTION_REPO_NAME = "Add the %s repository in which to run the task.";
    public static final String DESCRIPTION_REPOSITORY_PATH = "Enter regex for a repository path to run the task in recursively (ie. \"org\\/apache\\/.*\" for \"org/apache/*\"). Blank will not filter based off path";
    public static final String DESCRIPTION_FILE_PATTERN_MATCH = "The file patterns to filter the assets for the task.";
    public static final String DESCRIPTION_TASK_WORKING_DIRECTORY = "The parent directory where the Black Duck directory will be created to store data";
    public static final String DESCRIPTION_ASSET_CUTOFF = "If this is set, only assets with a modified date later than specified will be retrieved for the task. To get only assets newer than January 01, 2016 you would use "
                                                              + "the cutoff format of \"2016-01-01T00:00:00.000\"";

    public static RepositoryCombobox getRepositoryField(String... repoTypes) {
        Set<String> listedRepoTypes = Arrays.stream(repoTypes).collect(Collectors.toSet());
        String[] allRepoTypes = { GroupType.NAME, ProxyType.NAME, HostedType.NAME };
        String[] excludedRepoTypes = Arrays.stream(allRepoTypes).filter(repoType -> !listedRepoTypes.contains(repoType)).toArray(String[]::new);
        String allRepos = String.join(" or ", repoTypes);
        return new RepositoryCombobox(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, CommonDescriptorHelper.LABEL_REPOSITORY, String.format(CommonDescriptorHelper.DESCRIPTION_REPO_NAME, allRepos), FormField.MANDATORY)
                   .excludingAnyOfTypes(excludedRepoTypes);
    }

    public static StringTextFormField getRepositoryPathField() {
        return new StringTextFormField(CommonTaskKeys.REPOSITORY_PATH.getParameterKey(), CommonDescriptorHelper.LABEL_REPOSITORY_PATH, CommonDescriptorHelper.DESCRIPTION_REPOSITORY_PATH, FormField.OPTIONAL);
    }

    public static StringTextFormField getFilePatternField() {
        return new StringTextFormField(CommonTaskKeys.FILE_PATTERNS.getParameterKey(), CommonDescriptorHelper.LABEL_FILE_PATTERN_MATCHES, CommonDescriptorHelper.DESCRIPTION_FILE_PATTERN_MATCH, FormField.MANDATORY);
    }

    public static StringTextFormField getWorkingDirectoryField() {
        return new StringTextFormField(CommonTaskKeys.WORKING_DIRECTORY.getParameterKey(), CommonDescriptorHelper.LABEL_WORKING_DIRECTORY, CommonDescriptorHelper.DESCRIPTION_TASK_WORKING_DIRECTORY, FormField.MANDATORY)
                   .withInitialValue(CommonDescriptorHelper.DEFAULT_WORKING_DIRECTORY);
    }

    public static StringTextFormField getAssetCutoffDateField() {
        return new StringTextFormField(CommonTaskKeys.OLD_ASSET_CUTOFF.getParameterKey(), CommonDescriptorHelper.LABEL_ASSET_CUTOFF, CommonDescriptorHelper.DESCRIPTION_ASSET_CUTOFF, FormField.OPTIONAL)
                   .withInitialValue(CommonDescriptorHelper.DEFAULT_ARTIFACT_CUTOFF);
    }

    private CommonDescriptorHelper() {
        throw new IllegalStateException("Utility class");
    }

}
