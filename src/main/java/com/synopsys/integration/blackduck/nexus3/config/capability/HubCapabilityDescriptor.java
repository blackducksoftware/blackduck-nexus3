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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capability.CapabilityDescriptorSupport;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.StringTextFormField;

import com.synopsys.integration.blackduck.nexus3.config.HubServerConfig;

@Singleton
@Named(HubCapabilityDescriptor.CAPABILITY_ID)
public class HubCapabilityDescriptor extends CapabilityDescriptorSupport<HubCapabilityConfiguration> {
    public static final String CAPABILITY_ID = "HubCapability";
    public static final String CAPABILITY_NAME = "Black Duck Hub";
    public static final String CAPABILITY_DESCRIPTION = "Settings required to communicate with the Black Duck Hub.";

    private final CapabilityRegistry capabilityRegistry;
    private final HubServerConfig hubServerConfig;

    @Inject
    public HubCapabilityDescriptor(final CapabilityRegistry capabilityRegistry, final HubServerConfig hubServerConfig) {
        this.capabilityRegistry = capabilityRegistry;
        this.hubServerConfig = hubServerConfig;

        final Map<String, String> testMap = new HashMap<>();
        testMap.put("test", "empty");
        this.capabilityRegistry.add(type(), true, "Test Notes", testMap);
    }

    public CapabilityIdentity id() {
        return CapabilityIdentity.capabilityIdentity(CAPABILITY_ID);
    }

    @Override
    public CapabilityType type() {
        return CapabilityType.capabilityType(CAPABILITY_NAME);
    }

    @Override
    public String name() {
        return CAPABILITY_NAME;
    }

    @Override
    public String about() {
        return CAPABILITY_DESCRIPTION;
    }

    @Override
    public List<FormField> formFields() {
        return Arrays.asList(new StringTextFormField(
                "test",
                "Test Text",
                "BlackDuck test field",
                FormField.OPTIONAL));
    }

    // public CapabilityRegistry getCapabilityRegistry() {
    // return capabilityRegistry;
    // }
    //
    // public HubServerConfig getHubServerConfig() {
    // return hubServerConfig;
    // }

}

// @Inject
// public HubCapabilityDescriptor(final CapabilityRegistry capabilityRegistry, final HubServerConfig hubServerConfig) {
// this.capabilityRegistry = capabilityRegistry;
// this.hubServerConfig = hubServerConfig;
// }
//
// @Override
// public void validate(final CapabilityIdentity id, final Map<String, String> properties, final ValidationMode validationMode) {
// // TODO Auto-generated method stub
//
// }
//
// @Override
// public int version() {
// // TODO Auto-generated method stub
// return 0;
// }
//
// @Override
// public Map<String, String> convert(final Map<String, String> properties, final int fromVersion) {
// // TODO Auto-generated method stub
// return null;
// }
