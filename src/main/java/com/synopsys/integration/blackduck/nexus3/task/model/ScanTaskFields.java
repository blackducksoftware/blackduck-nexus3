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

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.repository.RepositoryTaskSupport;

public class ScanTaskFields {
    public static final String DEFAULT_FILE_PATTERNS_MATCHES = "*.war,*.zip,*.tar.gz,*.hpi";
    public static final String DEFAULT_WORKING_DIRECTORY = "/sonatype-work";

    public static final String LABEL_REPOSITORY = "Repository";
    public static final String LABEL_FILE_PATTERN_MATCHES = "File Pattern Matches";
    public static final String LABEL_WORKING_DIRECTORY = "Working Directory";

    public static final String DESCRIPTION_REPO_NAME = "Type in the repository in which to run the task.";
    public static final String DESCRIPTION_SCAN_FILE_PATTERN_MATCH = "The file pattern match wildcard to filter the artifacts scanned.";
    public static final String DESCRIPTION_TASK_WORKING_DIRECTORY = "The parent directory where the blackduck directory will be created to contain temporary data for the scans";

    public static final RepositoryCombobox FIELD_REPOSITORY = new RepositoryCombobox(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, LABEL_REPOSITORY, DESCRIPTION_REPO_NAME, FormField.MANDATORY);
    public static final StringTextFormField FIELD_FILE_PATTERN = new StringTextFormField(ScanTaskField.FILE_PATTERNS.getParameterKey(), LABEL_FILE_PATTERN_MATCHES, DESCRIPTION_SCAN_FILE_PATTERN_MATCH, FormField.MANDATORY)
                                                                     .withInitialValue(DEFAULT_FILE_PATTERNS_MATCHES);
    public static final StringTextFormField FIELD_WORKING_DIRECTORY = new StringTextFormField(ScanTaskField.WORKING_DIRECTORY.getParameterKey(), LABEL_WORKING_DIRECTORY, DESCRIPTION_TASK_WORKING_DIRECTORY, FormField.MANDATORY)
                                                                          .withInitialValue(DEFAULT_WORKING_DIRECTORY);

}
