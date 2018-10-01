package com.synopsys.integration.blackduck.nexus3.task;

import java.util.Optional;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.capability.HubCapabilityConfiguration;
import com.synopsys.integration.blackduck.nexus3.capability.HubCapabilityFinder;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;

@Named
@Singleton
public class CommonRepositoryTaskHelper {
    private final HubCapabilityFinder hubCapabilityFinder;
    private final QueryManager queryManager;

    @Inject
    public CommonRepositoryTaskHelper(final HubCapabilityFinder hubCapabilityFinder, final QueryManager queryManager) {
        this.hubCapabilityFinder = hubCapabilityFinder;
        this.queryManager = queryManager;
    }

    // TODO verify that the group repository will work accordingly here
    public boolean doesRepositoryApply(final Repository repository, final String repositoryField) {
        final String repositoryName = repositoryField;
        return repository.getName().equals(repositoryName);
    }

    public String getTaskMessage(final String taskName, final String repositoryField) {
        return String.format("Running BlackDuck %s for repository %s: ", taskName, repositoryField);
    }

    public HubServerConfig getHubServerConfig() {
        final HubCapabilityConfiguration hubCapabilityConfiguration = hubCapabilityFinder.retrieveHubCapabilityConfiguration();
        if (hubCapabilityConfiguration == null) {
            throw new TaskInterruptedException("BlackDuck hub server config not set.", true);
        }
        return hubCapabilityConfiguration.createHubServerConfig();
    }

    public PagedResult<Asset> pagedAssets(final Repository repository, final Query.Builder filteredQueryBuilder, final Optional<String> lastNameUsed, final int limit) {
        final String nameWhereStatement = lastNameUsed.isPresent() ? "name > " + lastNameUsed.get() : null;
        if (StringUtils.isNotBlank(nameWhereStatement)) {
            if (filteredQueryBuilder.hasWhere()) {
                filteredQueryBuilder.and(nameWhereStatement);
            } else {
                filteredQueryBuilder.where(nameWhereStatement);
            }
        }

        filteredQueryBuilder.suffix(String.format("ORDER BY name LIMIT %d", limit));

        final Iterable<Asset> filteredAssets = queryManager.findAssetsInRepository(repository, filteredQueryBuilder.build());
        final Optional<Asset> lastReturnedAsset = StreamSupport.stream(filteredAssets.spliterator(), true).reduce((first, second) -> second);
        Optional<String> name = Optional.empty();
        if (lastReturnedAsset.isPresent()) {
            name = Optional.of(lastReturnedAsset.get().name());
        }
        return new PagedResult<>(filteredAssets, name, limit);
    }

}
