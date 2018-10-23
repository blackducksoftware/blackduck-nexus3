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
package com.synopsys.integration.blackduck.nexus3.task.inspector;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import com.synopsys.integration.blackduck.nexus3.task.common.CommonDescriptorHelper;

@Named
@Singleton
public class InspectorTaskDescriptor extends TaskDescriptorSupport {
    public static final String BLACK_DUCK_INSPECTOR_TASK_ID = "blackduck.inspector";
    public static final String BLACK_DUCK_INSPECTOR_TASK_NAME = "Black Duck - Proxy Repository Inspector";

    public InspectorTaskDescriptor() {
        super(BLACK_DUCK_INSPECTOR_TASK_ID,
            InspectorTask.class,
            BLACK_DUCK_INSPECTOR_TASK_NAME,
            VISIBLE,
            EXPOSED,
            getFields()
        );
    }

    public static FormField[] getFields() {
        final FormField[] fields = {
            CommonDescriptorHelper.getRepositoryField(ProxyType.NAME),
            CommonDescriptorHelper.getRepositoryPathField(),
            CommonDescriptorHelper.getFilePatternField(),
            CommonDescriptorHelper.getWorkingDirectoryField(),
            CommonDescriptorHelper.getAssetCutoffDateField()
        };
        return fields;
    }

}
