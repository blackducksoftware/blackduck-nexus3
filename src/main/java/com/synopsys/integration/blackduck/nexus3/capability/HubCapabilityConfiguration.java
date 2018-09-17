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
package com.synopsys.integration.blackduck.nexus3.capability;

import java.util.Map;

import org.sonatype.nexus.capability.CapabilityConfigurationSupport;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.configuration.HubServerConfigBuilder;
import com.synopsys.integration.blackduck.nexus3.capability.model.HubConfigFields;

public class HubCapabilityConfiguration extends CapabilityConfigurationSupport {
    private final Map<String, String> capabilitySettings;

    public HubCapabilityConfiguration(final Map<String, String> capabilitySettings) {
        this.capabilitySettings = capabilitySettings;
    }

    public HubServerConfig createHubServerConfig() {
        final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
        hubServerConfigBuilder.setUrl(capabilitySettings.get(HubConfigFields.HUB_URL.getKey()));
        hubServerConfigBuilder.setTimeout(capabilitySettings.get(HubConfigFields.HUB_TIMEOUT.getKey()));
        hubServerConfigBuilder.setTrustCert(capabilitySettings.get(HubConfigFields.HUB_TRUST_CERT.getKey()));
        hubServerConfigBuilder.setUsername(capabilitySettings.get(HubConfigFields.HUB_USERNAME.getKey()));
        hubServerConfigBuilder.setPassword(capabilitySettings.get(HubConfigFields.HUB_PASSWORD.getKey()));
        return hubServerConfigBuilder.build();
    }

}
