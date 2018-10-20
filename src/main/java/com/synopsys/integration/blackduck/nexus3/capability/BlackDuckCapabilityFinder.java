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

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilitySupport;

@Named
@Singleton
public class BlackDuckCapabilityFinder {
    private final CapabilityRegistry capabilityRegistry;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    public BlackDuckCapabilityFinder(final CapabilityRegistry capabilityRegistry) {
        this.capabilityRegistry = capabilityRegistry;
    }

    public BlackDuckCapabilityConfiguration retrieveBlackDuckCapabilityConfiguration() {
        final CapabilityReference capabilityReference = findCapabilityReference(BlackDuckCapability.class);
        if (capabilityReference == null) {
            logger.warn("BlackDuck capability not created.");
            return null;
        }

        final BlackDuckCapability capability = capabilityReference.capabilityAs(BlackDuckCapability.class);
        logger.info("Getting BlackDuckCapability config");
        return capability.getConfig();
    }

    //TODO Find a better way to get the correct capability (Don't know what the ID is to use with get)
    public CapabilityReference findCapabilityReference(final Class<? extends CapabilitySupport<?>> capabilityClass) {
        final Collection<? extends CapabilityReference> capabilityReferenceList = capabilityRegistry.getAll();
        for (final CapabilityReference capabilityReference : capabilityReferenceList) {
            final String capabilityName = capabilityReference.capability().getClass().getName();
            if (capabilityName.equals(capabilityClass.getName())) {
                logger.debug("Found capability: " + capabilityName);
                return capabilityReference;
            }
        }
        return null;
    }
}
