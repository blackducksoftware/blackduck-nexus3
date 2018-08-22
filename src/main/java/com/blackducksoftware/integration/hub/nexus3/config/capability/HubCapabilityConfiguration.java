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

import javax.inject.Named;

import org.sonatype.nexus.capability.CapabilityConfigurationSupport;
import org.sonatype.nexus.formfields.StringTextFormField;

// This class is required for FormField conversion
@Named(HubCapabilityDescriptor.CAPABILITY_ID)
public class HubCapabilityConfiguration extends CapabilityConfigurationSupport {
    // private final HubServerConfig hubServerConfig;
    //
    // public HubCapabilityConfiguration(final HubServerConfig hubServerConfig) {
    // this.hubServerConfig = hubServerConfig;
    // }

    public StringTextFormField test;

    public HubCapabilityConfiguration() {
    }

}
