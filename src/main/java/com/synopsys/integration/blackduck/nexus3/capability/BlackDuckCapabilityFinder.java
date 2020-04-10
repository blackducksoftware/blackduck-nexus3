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
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityType;

@Named
@Singleton
public class BlackDuckCapabilityFinder {
    private final CapabilityRegistry capabilityRegistry;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    public BlackDuckCapabilityFinder(CapabilityRegistry capabilityRegistry) {
        this.capabilityRegistry = capabilityRegistry;
    }

    public BlackDuckCapabilityConfiguration retrieveBlackDuckCapabilityConfiguration() {
        Optional<? extends CapabilityReference> capabilityReferenceOptional = findCapabilityReference();
        if (!capabilityReferenceOptional.isPresent()) {
            logger.warn("Black Duck capability not created.");
            return null;
        }
        BlackDuckCapability capability = capabilityReferenceOptional.get().capabilityAs(BlackDuckCapability.class);
        logger.info("Retrieved the Black Duck capability config");
        return capability.getConfig();
    }

    public Optional<? extends CapabilityReference> findCapabilityReference() {
        CapabilityType blackDuckCapabilityType = CapabilityType.capabilityType(BlackDuckCapabilityDescriptor.CAPABILITY_ID);
        Collection<? extends CapabilityReference> capabilityReferences = capabilityRegistry.get(capabilityReference -> matchesBlackDuckCapabilityType(capabilityReference, blackDuckCapabilityType));
        if (capabilityReferences.isEmpty()) {
            return Optional.empty();
        }
        return capabilityReferences.stream().findFirst();
    }

    private boolean matchesBlackDuckCapabilityType(CapabilityReference capabilityReference, CapabilityType blackDuckCapabilityType) {
        if (null != capabilityReference && null != capabilityReference.context() && null != capabilityReference.context().type()) {
            return capabilityReference.context().type().equals(blackDuckCapabilityType);
        }
        return false;
    }
}
