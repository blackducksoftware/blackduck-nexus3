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
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import com.synopsys.integration.blackduck.nexus3.task.common.CommonDescriptorHelper;

@Named
@Singleton
public class ScanTaskDescriptor extends TaskDescriptorSupport {
    public static final String BLACK_DUCK_SCAN_TASK_ID = "blackduck.scan";
    public static final String BLACK_DUCK_SCAN_TASK_NAME = "Black Duck - Hosted Repository Scan";
    public static final int DEFAULT_SCAN_MEMORY = 4096;
    public static final String KEY_SCAN_MEMORY = "blackduck.memory";
    public static final String KEY_REDO_FAILURES = "blackduck.redo.failures";
    public static final String KEY_ALWAYS_CHECK = "blackduck.check.always";
    private static final String LABEL_SCAN_MEMORY = "Black Duck - Scan memory Allocation";
    private static final String LABEL_ALWAYS_SCAN = "Black Duck - Scan Successful and Pending Assets";
    private static final String LABEL_RESCAN_FAILURE = "Black Duck - Scan Failed Assets";
    private static final String DESCRIPTION_SCAN_MEMORY = "Specify the memory, in megabytes, you would like to allocate for the Black Duck Scan. Default: 4096";
    private static final String DESCRIPTION_ALWAYS_SCAN = "Scan Successful or Pending asset as long as they are not too old and match the specified patterns";
    private static final String DESCRIPTION_RESCAN_FAILURE = "Scan asset if the previous scan result was failed";
    private static final NumberTextFormField FIELD_SCAN_MEMORY = new NumberTextFormField(KEY_SCAN_MEMORY, LABEL_SCAN_MEMORY, DESCRIPTION_SCAN_MEMORY, FormField.MANDATORY)
                                                                     .withInitialValue(DEFAULT_SCAN_MEMORY);
    private static final CheckboxFormField FIELD_ALWAYS_SCAN = new CheckboxFormField(KEY_ALWAYS_CHECK, LABEL_ALWAYS_SCAN, DESCRIPTION_ALWAYS_SCAN, FormField.OPTIONAL);
    private static final CheckboxFormField FIELD_RESCAN_FAILURE = new CheckboxFormField(KEY_REDO_FAILURES, LABEL_RESCAN_FAILURE, DESCRIPTION_RESCAN_FAILURE, FormField.OPTIONAL);

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
        final String repoTypes = HostedType.NAME;
        final FormField[] fields = {
            CommonDescriptorHelper.getRepositoryField(repoTypes),
            CommonDescriptorHelper.getRepositoryPathField(),
            CommonDescriptorHelper.getFilePatternField(),
            CommonDescriptorHelper.getWorkingDirectoryField(),
            FIELD_SCAN_MEMORY,
            FIELD_ALWAYS_SCAN,
            FIELD_RESCAN_FAILURE,
            CommonDescriptorHelper.getAssetCutoffDateField()
        };
        return fields;
    }

}
