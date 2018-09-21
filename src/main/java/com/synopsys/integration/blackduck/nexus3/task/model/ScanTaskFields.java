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

import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.repository.RepositoryTaskSupport;

public class ScanTaskFields {
    public static final String DEFAULT_FILE_PATTERNS_MATCHES = "*.war,*.zip,*.tar.gz,*.hpi";
    public static final String DEFAULT_WORKING_DIRECTORY = "../sonatype-work";
    public static final int DEFAULT_SCAN_MEMORY = 4096;
    public static final String DEFAULT_ARTIFACT_CUTOFF = "2016-01-01T00:00:00.000";

    public static final String LABEL_REPOSITORY = "Repository";
    public static final String LABEL_FILE_PATTERN_MATCHES = "File Pattern Matches";
    public static final String LABEL_WORKING_DIRECTORY = "Working Directory";
    public static final String LABEL_SCAN_MEMORY = "Scan memory Allocation";
    public static final String LABEL_ALWAYS_SCAN = "Always Scan Artifacts";
    public static final String LABEL_RESCAN_FAILURE = "Re-scan Failed Scan Attempts";
    public static final String LABEL_ARTIFACT_CUTOFF = "Artifact Cutoff Date";

    public static final String DESCRIPTION_REPO_NAME = "Type in the repository in which to run the task.";
    public static final String DESCRIPTION_SCAN_FILE_PATTERN_MATCH = "The file pattern match wildcard to filter the artifacts scanned.";
    public static final String DESCRIPTION_TASK_WORKING_DIRECTORY = "The parent directory where the blackduck directory will be created to contain temporary data for the scans";
    public static final String DESCRIPTION_SCAN_MEMORY = "Specify the memory, in megabytes, you would like to allocate for the BlackDuck Scan. Default: 4096";
    public static final String DESCRIPTION_ALWAYS_SCAN = "Always scan artifacts that are not too old and match the file pattern, regardless of previous scan result";
    public static final String DESCRIPTION_RESCAN_FAILURE = "Re-scan artifacts if the previous scan result was failed";
    public static final String DESCRIPTION_SCAN_CUTOFF = "If this is set, only artifacts with a modified date later than this will be scanned. To scan only artifacts newer than January 01, 2016 you would use "
                                                             + "the cutoff format of \"2016-01-01T00:00:00.000\"";

    public static final RepositoryCombobox FIELD_REPOSITORY = new RepositoryCombobox(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, LABEL_REPOSITORY, DESCRIPTION_REPO_NAME, FormField.MANDATORY);
    public static final StringTextFormField FIELD_FILE_PATTERN = new StringTextFormField(ScanTaskKeys.FILE_PATTERNS.getParameterKey(), LABEL_FILE_PATTERN_MATCHES, DESCRIPTION_SCAN_FILE_PATTERN_MATCH, FormField.MANDATORY)
                                                                     .withInitialValue(DEFAULT_FILE_PATTERNS_MATCHES);
    public static final StringTextFormField FIELD_WORKING_DIRECTORY = new StringTextFormField(ScanTaskKeys.WORKING_DIRECTORY.getParameterKey(), LABEL_WORKING_DIRECTORY, DESCRIPTION_TASK_WORKING_DIRECTORY, FormField.MANDATORY)
                                                                          .withInitialValue(DEFAULT_WORKING_DIRECTORY);
    public static final NumberTextFormField FIELD_SCAN_MEMORY = new NumberTextFormField(ScanTaskKeys.SCAN_MEMORY.getParameterKey(), LABEL_SCAN_MEMORY, DESCRIPTION_SCAN_MEMORY, FormField.MANDATORY)
                                                                    .withInitialValue(DEFAULT_SCAN_MEMORY);
    public static final CheckboxFormField FIELD_ALWAYS_SCAN = new CheckboxFormField(ScanTaskKeys.ALWAYS_SCAN.getParameterKey(), LABEL_ALWAYS_SCAN, DESCRIPTION_ALWAYS_SCAN, FormField.OPTIONAL);
    public static final CheckboxFormField FIELD_RESCAN_FAILURE = new CheckboxFormField(ScanTaskKeys.RESCAN_FAILURES.getParameterKey(), LABEL_RESCAN_FAILURE, DESCRIPTION_RESCAN_FAILURE, FormField.OPTIONAL);
    public static final StringTextFormField FIELD_ARTIFACT_CUTOFF = new StringTextFormField(ScanTaskKeys.OLD_ARTIFACT_CUTOFF.getParameterKey(), LABEL_ARTIFACT_CUTOFF, DESCRIPTION_SCAN_CUTOFF, FormField.OPTIONAL)
                                                                        .withInitialValue(DEFAULT_ARTIFACT_CUTOFF);

    public static FormField[] getFields() {
        final FormField[] fields = { FIELD_REPOSITORY, FIELD_WORKING_DIRECTORY, FIELD_FILE_PATTERN, FIELD_SCAN_MEMORY, FIELD_ALWAYS_SCAN, FIELD_RESCAN_FAILURE, FIELD_ARTIFACT_CUTOFF };
        return fields;
    }
}
