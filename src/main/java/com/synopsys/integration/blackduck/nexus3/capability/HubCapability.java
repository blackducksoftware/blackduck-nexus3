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

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.capability.CapabilitySupport;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.util.BlackDuckConnection;

@Named(HubCapabilityDescriptor.CAPABILITY_ID)
public class HubCapability extends CapabilitySupport<HubCapabilityConfiguration> {
    BlackDuckConnection blackDuckConnection;

    @Inject
    public HubCapability(final BlackDuckConnection blackDuckConnection) {
        this.blackDuckConnection = blackDuckConnection;
    }

    @Override
    protected HubCapabilityConfiguration createConfig(final Map<String, String> properties) {
        return new HubCapabilityConfiguration(properties);
    }

    @Override
    public boolean isPasswordProperty(final String propertyName) {
        return HubConfigKeys.passwordFields().stream().anyMatch(field -> field.getKey().equals(propertyName));
    }

    @Override
    protected void configure(final HubCapabilityConfiguration config) throws Exception {
        log.debug("Configuring HubCapability");
        super.configure(config);
        final HubServerConfig hubServerConfig = config.createHubServerConfig();
        blackDuckConnection.setHubServerConfig(hubServerConfig);
    }
}
