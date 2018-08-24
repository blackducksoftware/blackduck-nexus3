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
package com.synopsys.integration.blackduck.nexus3.config.capability;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capability.CapabilityDescriptorSupport;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.TextAreaFormField;

@Singleton
@Named(HubCapabilityDescriptor.CAPABILITY_ID)
public class HubCapabilityDescriptor extends CapabilityDescriptorSupport<HubCapabilityConfiguration> {
    public static final String CAPABILITY_ID = "HubCapability";
    public static final String CAPABILITY_NAME = "Black Duck Hub";
    public static final String CAPABILITY_DESCRIPTION = "Settings required to communicate with the Black Duck Hub.";

    // TODO make final
    // private final CapabilityRegistry capabilityRegistry;
    // private final HubServerConfig hubServerConfig;

    public HubCapabilityDescriptor() {
        // this.capabilityRegistry = capabilityRegistry;
        // this.hubServerConfig = hubServerConfig;
        //
        // final Map<String, String> testMap = new HashMap<>();
        // testMap.put("test_map_key", "empty");
        // capabilityRegistry.add(type(), true, "Test Notes", testMap);
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
        return Arrays.asList(new TextAreaFormField(
                "headerHtml",
                "Header HTML snippet",
                "An HTML snippet to be included in branding header.<br/>" + "Use '$baseUrl' to insert the base URL of the server (e.g. to reference an image)",
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
