package com.synopsys.integration.blackduck.nexus3.task;

import org.slf4j.Logger;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.capability.HubCapabilityConfiguration;
import com.synopsys.integration.blackduck.nexus3.capability.HubCapabilityFinder;

public abstract class BlackDuckTask extends RepositoryTaskSupport {
    private final String taskName;
    private final HubCapabilityFinder hubCapabilityFinder;
    private final Logger logger = createLogger();

    public BlackDuckTask(final String taskName, final HubCapabilityFinder hubCapabilityFinder) {
        this.taskName = taskName;
        this.hubCapabilityFinder = hubCapabilityFinder;
    }

    // TODO verify that the group repository will work accordingly here
    @Override
    protected boolean appliesTo(final Repository repository) {
        final String repositoryName = getRepositoryField();
        return repository.getName().equals(repositoryName);
    }

    @Override
    public String getMessage() {
        return String.format("Running BlackDuck %s for repository %s: ", taskName, getRepositoryField());
    }

    @Override
    protected void execute(final Repository repository) {
        logger.debug("Found repository: " + repository.getName());
        execute(repository, getHubServerConfig());
    }

    protected abstract void execute(Repository repository, HubServerConfig hubServerConfig);

    private HubServerConfig getHubServerConfig() {
        final HubCapabilityConfiguration hubCapabilityConfiguration = hubCapabilityFinder.retrieveHubCapabilityConfiguration();
        if (hubCapabilityConfiguration == null) {
            throw new TaskInterruptedException("BlackDuck hub server config not set.", true);
        }
        return hubCapabilityConfiguration.createHubServerConfig();
    }

}
