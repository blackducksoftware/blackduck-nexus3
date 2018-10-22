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

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.repository.RepositoryTaskSupport;

public class CommonDescriptorHelper {
    public static final String DEFAULT_FILE_PATTERNS_MATCHES = "*.war,*.zip,*.tar.gz,*.hpi";
    public static final String DEFAULT_WORKING_DIRECTORY = "../sonatype-work";
    public static final String DEFAULT_ARTIFACT_CUTOFF = "2016-01-01T00:00:00.000";
    public static final int DEFAULT_PAGE_SIZE = 100;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MIN_PAGE_SIZE = 1;

    public static final String LABEL_REPOSITORY = "Repository";
    public static final String LABEL_REPOSITORY_PATH = "Repository Path";
    public static final String LABEL_FILE_PATTERN_MATCHES = "File Pattern Matches";
    public static final String LABEL_WORKING_DIRECTORY = "Working Directory";
    public static final String LABEL_ARTIFACT_CUTOFF = "Artifact Cutoff Date";
    public static final String LABEL_PAGE_SIZE = "Items to keep in memory";

    public static final String DESCRIPTION_REPO_NAME = "Add the repository in which to run the task.";
    public static final String DESCRIPTION_REPOSITORY_PATH = "Enter a repository path to run the task in recursively (ie. \"/\" for root or \"/org/apache\"). Blank will not filter based off path";
    public static final String DESCRIPTION_FILE_PATTERN_MATCH = "The file pattern match wildcard to filter the artifacts for the task.";
    public static final String DESCRIPTION_TASK_WORKING_DIRECTORY = "The parent directory where the BlackDuck directory will be created to store data";
    public static final String DESCRIPTION_ARTIFACT_CUTOFF = "If this is set, only artifacts with a modified date later than specified will be retrieved for the task. To get only artifacts newer than January 01, 2016 you would use "
                                                                 + "the cutoff format of \"2016-01-01T00:00:00.000\"";
    public static final String DESCRIPTION_PAGE_SIZE = "Use to limit the number of items we retrieve from the Database at one time. A maximum value of 100 and a minimum of 1 are allowed";

    public static RepositoryCombobox getRepositoryField() {
        return new RepositoryCombobox(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, CommonDescriptorHelper.LABEL_REPOSITORY, CommonDescriptorHelper.DESCRIPTION_REPO_NAME, FormField.MANDATORY);
    }

    public static StringTextFormField getRepositoryPathField() {
        return new StringTextFormField(CommonTaskKeys.REPOSITORY_PATH.getParameterKey(), CommonDescriptorHelper.LABEL_REPOSITORY_PATH, CommonDescriptorHelper.DESCRIPTION_REPOSITORY_PATH, FormField.OPTIONAL);
    }

    public static StringTextFormField getFilePatternField() {
        return new StringTextFormField(CommonTaskKeys.FILE_PATTERNS.getParameterKey(), CommonDescriptorHelper.LABEL_FILE_PATTERN_MATCHES, CommonDescriptorHelper.DESCRIPTION_FILE_PATTERN_MATCH, FormField.MANDATORY)
                   .withInitialValue(CommonDescriptorHelper.DEFAULT_FILE_PATTERNS_MATCHES);
    }

    public static StringTextFormField getWorkingDirectoryField() {
        return new StringTextFormField(CommonTaskKeys.WORKING_DIRECTORY.getParameterKey(), CommonDescriptorHelper.LABEL_WORKING_DIRECTORY, CommonDescriptorHelper.DESCRIPTION_TASK_WORKING_DIRECTORY, FormField.MANDATORY)
                   .withInitialValue(CommonDescriptorHelper.DEFAULT_WORKING_DIRECTORY);
    }

    public static StringTextFormField getArtifactCutoffDateField() {
        return new StringTextFormField(CommonTaskKeys.OLD_ARTIFACT_CUTOFF.getParameterKey(), CommonDescriptorHelper.LABEL_ARTIFACT_CUTOFF, CommonDescriptorHelper.DESCRIPTION_ARTIFACT_CUTOFF, FormField.OPTIONAL)
                   .withInitialValue(CommonDescriptorHelper.DEFAULT_ARTIFACT_CUTOFF);
    }

    public static NumberTextFormField getPageSizeLimitField() {
        return new NumberTextFormField(CommonTaskKeys.PAGING_SIZE.getParameterKey(), CommonDescriptorHelper.LABEL_PAGE_SIZE, CommonDescriptorHelper.DESCRIPTION_PAGE_SIZE, FormField.MANDATORY)
                   .withInitialValue(CommonDescriptorHelper.DEFAULT_PAGE_SIZE).withMinimumValue(CommonDescriptorHelper.MIN_PAGE_SIZE)
                   .withMaximumValue(CommonDescriptorHelper.MAX_PAGE_SIZE);
    }

}
