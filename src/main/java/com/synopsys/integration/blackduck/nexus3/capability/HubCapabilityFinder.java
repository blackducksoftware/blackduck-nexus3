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
public class HubCapabilityFinder {
    private final CapabilityRegistry capabilityRegistry;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    public HubCapabilityFinder(final CapabilityRegistry capabilityRegistry) {
        this.capabilityRegistry = capabilityRegistry;
    }

    public HubCapabilityConfiguration retrieveHubCapabilityConfiguration() {
        final CapabilityReference capabilityReference = findCapabilityReference(HubCapability.class);
        if (capabilityReference == null) {
            logger.warn("Hub capability not created.");
            return null;
        }

        final HubCapability capability = capabilityReference.capabilityAs(HubCapability.class);
        logger.info("Getting HubCapability config");
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
