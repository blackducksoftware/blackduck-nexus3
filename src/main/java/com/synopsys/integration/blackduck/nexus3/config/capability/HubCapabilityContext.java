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
package com.synopsys.integration.blackduck.nexus3.config.capability;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.capability.CapabilityType;

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
        // final HubServerConfig config = descriptor.getHubServerConfig();
        // try {
        // return config.getConfigMapAsPlainText();
        // } catch (final IntegrationException e) {
        // logger.error("Cannot retrieve Black Duck Hub Configuration", e);
        // }
        // final Map<String, String> map = new HashMap<>();
        // Arrays.asList(HubServerField.values()).forEach(field -> map.put(field.getKey(), ""));
        // return map;
        return null;
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
