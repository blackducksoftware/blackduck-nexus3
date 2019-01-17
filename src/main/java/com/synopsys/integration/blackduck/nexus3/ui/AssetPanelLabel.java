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
package com.synopsys.integration.blackduck.nexus3.ui;

public enum AssetPanelLabel {
    OLD_STATUS("status"),
    SCAN_TASK_STATUS("scan_status"),
    INSPECTION_TASK_STATUS("inspection_status"),
    TASK_STATUS_DESCRIPTION("status_description"),
    BLACKDUCK_URL("blackduck_url"),
    TASK_FINISHED_TIME("processed_on"),
    ASSET_ORIGIN_ID("origin_id"),
    OVERALL_POLICY_STATUS("policy_status_overall"),
    POLICY_STATUS("policy_status"),
    VULNERABILITIES("vulnerabilities"),
    VULNERABLE_COMPONENTS("vulnerable_components");

    private final String label;

    AssetPanelLabel(final String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
