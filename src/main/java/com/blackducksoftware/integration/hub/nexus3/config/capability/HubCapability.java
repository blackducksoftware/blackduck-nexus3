/*
 * Copyright (C) 2018 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.hub.nexus3.config.capability;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.capability.CapabilitySupport;

import com.blackducksoftware.integration.hub.nexus3.config.HubServerConfig;
import com.blackducksoftware.integration.hub.nexus3.config.HubServerField;

@Named(HubCapabilityDescriptor.CAPABILITY_ID)
public class HubCapability extends CapabilitySupport<HubCapabilityConfiguration> {
    private final HubServerConfig hubServerConfig;

    @Inject
    public HubCapability(final HubServerConfig hubServerConfig) {
        this.hubServerConfig = hubServerConfig;
    }

    @Override
    protected HubCapabilityConfiguration createConfig(final Map<String, String> properties) throws Exception {
        return new HubCapabilityConfiguration(); // new HubCapabilityConfiguration(hubServerConfig);
    }

    @Override
    public boolean isPasswordProperty(final String propertyName) {
        return HubServerField.passwordFields().stream().anyMatch(field -> field.getKey().equals(propertyName));
    }

}
