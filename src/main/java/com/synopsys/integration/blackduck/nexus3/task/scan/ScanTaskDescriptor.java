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
package com.synopsys.integration.blackduck.nexus3.task.scan;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import com.synopsys.integration.blackduck.nexus3.task.CommonTaskKeys;

@Named
@Singleton
public class ScanTaskDescriptor extends TaskDescriptorSupport {
    public static final String BLACK_DUCK_SCAN_TASK_ID = "blackduck.scan";
    public static final String BLACK_DUCK_SCAN_TASK_NAME = "BlackDuck - Repository Scan";

    public static final String DEFAULT_FILE_PATTERNS_MATCHES = "*.war,*.zip,*.tar.gz,*.hpi";
    public static final String DEFAULT_WORKING_DIRECTORY = "../sonatype-work";
    public static final int DEFAULT_SCAN_MEMORY = 4096;
    public static final String DEFAULT_ARTIFACT_CUTOFF = "2016-01-01T00:00:00.000";
    public static final int DEFAULT_SCAN_PAGE_SIZE = 100;
    public static final int MAX_SCAN_PAGE_SIZE = 100;
    public static final int MIN_SCAN_PAGE_SIZE = 1;

    private static final String LABEL_REPOSITORY = "Repository";
    private static final String LABEL_REPOSITORY_PATH = "Repository Path";
    private static final String LABEL_FILE_PATTERN_MATCHES = "File Pattern Matches";
    private static final String LABEL_WORKING_DIRECTORY = "Working Directory";
    private static final String LABEL_SCAN_MEMORY = "Scan memory Allocation";
    private static final String LABEL_ALWAYS_SCAN = "Always Scan Artifacts";
    private static final String LABEL_RESCAN_FAILURE = "Re-scan Failed Scan Attempts";
    private static final String LABEL_ARTIFACT_CUTOFF = "Artifact Cutoff Date";
    private static final String LABEL_SCAN_PAGE_SIZE = "Items to keep in memory";

    private static final String DESCRIPTION_REPO_NAME = "Type in the repository in which to run the task.";
    private static final String DESCRIPTION_REPOSITORY_PATH = "Enter a repository path to run the task in recursively (ie. \"/\" for root or \"/org/apache\"). Blank will not filter based off path";
    private static final String DESCRIPTION_SCAN_FILE_PATTERN_MATCH = "The file pattern match wildcard to filter the artifacts scanned.";
    private static final String DESCRIPTION_TASK_WORKING_DIRECTORY = "The parent directory where the blackduck directory will be created to contain temporary data for the scans";
    private static final String DESCRIPTION_SCAN_MEMORY = "Specify the memory, in megabytes, you would like to allocate for the BlackDuck Scan. Default: 4096";
    // TODO verify that we want this to scan everything regardless of result, or just scan Success
    private static final String DESCRIPTION_ALWAYS_SCAN = "Always scan artifacts that are not too old and match the file pattern, regardless of previous scan result";
    private static final String DESCRIPTION_RESCAN_FAILURE = "Re-scan artifacts if the previous scan result was failed";
    private static final String DESCRIPTION_SCAN_CUTOFF = "If this is set, only artifacts with a modified date later than this will be scanned. To scan only artifacts newer than January 01, 2016 you would use "
                                                              + "the cutoff format of \"2016-01-01T00:00:00.000\"";
    private static final String DESCRIPTION_SCAN_PAGE_SIZE = "Use to limit the number of items we retrieve from the Database at one time. A maximum value of 100 and a minimum of 1 are allowed";

    private static final RepositoryCombobox FIELD_REPOSITORY = new RepositoryCombobox(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, LABEL_REPOSITORY, DESCRIPTION_REPO_NAME, FormField.MANDATORY).excludingAnyOfTypes(ProxyType.NAME);
    private static final StringTextFormField FIELD_REPOSITORY_PATH = new StringTextFormField(CommonTaskKeys.REPOSITORY_PATH.getParameterKey(), LABEL_REPOSITORY_PATH, DESCRIPTION_REPOSITORY_PATH, FormField.OPTIONAL);
    private static final StringTextFormField FIELD_FILE_PATTERN = new StringTextFormField(CommonTaskKeys.FILE_PATTERNS.getParameterKey(), LABEL_FILE_PATTERN_MATCHES, DESCRIPTION_SCAN_FILE_PATTERN_MATCH, FormField.MANDATORY)
                                                                      .withInitialValue(DEFAULT_FILE_PATTERNS_MATCHES);
    private static final StringTextFormField FIELD_WORKING_DIRECTORY = new StringTextFormField(CommonTaskKeys.WORKING_DIRECTORY.getParameterKey(), LABEL_WORKING_DIRECTORY, DESCRIPTION_TASK_WORKING_DIRECTORY, FormField.MANDATORY)
                                                                           .withInitialValue(DEFAULT_WORKING_DIRECTORY);
    private static final NumberTextFormField FIELD_SCAN_MEMORY = new NumberTextFormField(CommonTaskKeys.MAX_MEMORY.getParameterKey(), LABEL_SCAN_MEMORY, DESCRIPTION_SCAN_MEMORY, FormField.MANDATORY)
                                                                     .withInitialValue(DEFAULT_SCAN_MEMORY);
    private static final CheckboxFormField FIELD_ALWAYS_SCAN = new CheckboxFormField(CommonTaskKeys.ALWAYS_CHECK.getParameterKey(), LABEL_ALWAYS_SCAN, DESCRIPTION_ALWAYS_SCAN, FormField.OPTIONAL);
    private static final CheckboxFormField FIELD_RESCAN_FAILURE = new CheckboxFormField(CommonTaskKeys.REDO_FAILURES.getParameterKey(), LABEL_RESCAN_FAILURE, DESCRIPTION_RESCAN_FAILURE, FormField.OPTIONAL);
    private static final StringTextFormField FIELD_ARTIFACT_CUTOFF = new StringTextFormField(CommonTaskKeys.OLD_ARTIFACT_CUTOFF.getParameterKey(), LABEL_ARTIFACT_CUTOFF, DESCRIPTION_SCAN_CUTOFF, FormField.OPTIONAL)
                                                                         .withInitialValue(DEFAULT_ARTIFACT_CUTOFF);
    private static final NumberTextFormField FIELD_SCAN_PAGE_SIZE = new NumberTextFormField(CommonTaskKeys.PAGING_SIZE.getParameterKey(), LABEL_SCAN_PAGE_SIZE, DESCRIPTION_SCAN_PAGE_SIZE, FormField.MANDATORY)
                                                                        .withInitialValue(DEFAULT_SCAN_PAGE_SIZE).withMinimumValue(MIN_SCAN_PAGE_SIZE).withMaximumValue(MAX_SCAN_PAGE_SIZE);

    public ScanTaskDescriptor() {
        super(BLACK_DUCK_SCAN_TASK_ID,
            ScanTask.class,
            BLACK_DUCK_SCAN_TASK_NAME,
            VISIBLE,
            EXPOSED,
            getFields()
        );
    }

    public static FormField[] getFields() {
        final FormField[] fields = { FIELD_REPOSITORY, FIELD_REPOSITORY_PATH, FIELD_WORKING_DIRECTORY, FIELD_FILE_PATTERN, FIELD_SCAN_MEMORY, FIELD_ALWAYS_SCAN, FIELD_RESCAN_FAILURE, FIELD_ARTIFACT_CUTOFF, FIELD_SCAN_PAGE_SIZE };
        return fields;
    }

}
