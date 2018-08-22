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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.capability.CapabilityType;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.nexus3.config.HubServerConfig;
import com.blackducksoftware.integration.hub.nexus3.config.HubServerField;

@Named(HubCapabilityDescriptor.CAPABILITY_ID)
public class HubCapabilityContext implements CapabilityContext {
    private static final Logger logger = LoggerFactory.getLogger(HubCapabilityContext.class);
    private final HubCapabilityDescriptor descriptor;

    @Inject
    public HubCapabilityContext(final HubCapabilityDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public CapabilityIdentity id() {
        return descriptor.id();
    }

    @Override
    public CapabilityType type() {
        return descriptor.type();
    }

    @Override
    public CapabilityDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public String notes() {
        // TODO ?
        return "Black Duck by Synopsys";
    }

    @Override
    public Map<String, String> properties() {
        final HubServerConfig config = descriptor.getHubServerConfig();
        try {
            return config.getConfigMapAsPlainText();
        } catch (final IntegrationException e) {
            logger.error("Cannot retrieve Black Duck Hub Configuration", e);
        }
        final Map<String, String> map = new HashMap<>();
        Arrays.asList(HubServerField.values()).forEach(field -> map.put(field.getKey(), ""));
        return map;
    }

    @Override
    public boolean isEnabled() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean isActive() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean hasFailure() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Exception failure() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String failingAction() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String stateDescription() {
        // TODO Auto-generated method stub
        return null;
    }

}
