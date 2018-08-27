package com.synopsys.integration.blackduck.nexus3.task;

import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;

@Named
public class BlackDuckScanTask extends RepositoryTaskSupport {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public String getMessage() {
        return "BlackDuck scanning repository " + getRepositoryField();
    }

    @Override
    protected boolean appliesTo(final Repository repository) {
        final String repositoryName = getRepositoryField();
        return repository.getName().equals(repositoryName);
    }

    @Override
    protected void execute(final Repository repository) {
        logger.info("Found repository: " + repository.getName());
    }

}
