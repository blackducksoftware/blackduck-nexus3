package com.synopsys.integration.blackduck.nexus3.task.metadata;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.api.generated.view.VersionBomPolicyStatusView;
import com.synopsys.integration.blackduck.api.generated.view.VulnerabilityV2View;
import com.synopsys.integration.blackduck.api.generated.view.VulnerableComponentView;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.nexus3.util.AssetWrapper;
import com.synopsys.integration.exception.IntegrationException;

@Named
public class MetaDataTask extends RepositoryTaskSupport {
    public static final int PAGED_SIZE_LIMIT = 100;

    private final Logger logger = createLogger();
    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final QueryManager queryManager;
    private final MetaDataProcessor metaDataProcessor;

    @Inject
    public MetaDataTask(final CommonRepositoryTaskHelper commonRepositoryTaskHelper, final QueryManager queryManager, final MetaDataProcessor metaDataProcessor) {
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.queryManager = queryManager;
        this.metaDataProcessor = metaDataProcessor;
    }

    @Override
    protected void execute(final Repository repository) {
        final Query filteredAssets = createFilteredQuery(Optional.empty(), PAGED_SIZE_LIMIT);
        PagedResult<Asset> pagedAssets = commonRepositoryTaskHelper.pagedAssets(repository, filteredAssets);
        while (pagedAssets.hasResults()) {
            logger.debug("Found items in the DB.");
            for (final Asset asset : pagedAssets.getTypeList()) {
                final AssetWrapper assetWrapper = new AssetWrapper(asset, repository, queryManager);
                final String name = assetWrapper.getName();
                final String version = assetWrapper.getVersion();
                try {
                    final VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();
                    final List<VulnerableComponentView> vulnerableComponentViewList = metaDataProcessor.checkAssetVulnerabilities(name, version);
                    for (final VulnerableComponentView vulnerableComponentView : vulnerableComponentViewList) {
                        final List<VulnerabilityV2View> vulnerabilities = metaDataProcessor.convertToVulnerabilities(vulnerableComponentView);
                        vulnerabilityLevels.addVulnerabilityCounts(vulnerabilities);
                    }
                    metaDataProcessor.updateAssetVulnerabilityData(vulnerabilityLevels, assetWrapper);
                    final VersionBomPolicyStatusView policyStatusView = metaDataProcessor.checkAssetPolicy(name, version);
                    metaDataProcessor.updateAssetPolicyData(policyStatusView, assetWrapper);
                } catch (final IntegrationException e) {
                    metaDataProcessor.removeAssetVulnerabilityData(assetWrapper);
                    metaDataProcessor.removePolicyData(assetWrapper);
                    throw new TaskInterruptedException("Problem checking metadata: " + e.getMessage(), true);
                }
            }

            final Query nextPage = createFilteredQuery(pagedAssets.getLastName(), PAGED_SIZE_LIMIT);
            pagedAssets = commonRepositoryTaskHelper.pagedAssets(repository, nextPage);
        }
    }

    private Query createFilteredQuery(final Optional<String> lastNameUsed, final int limit) {
        final Query.Builder pagedQueryBuilder = commonRepositoryTaskHelper.createPagedQuery(lastNameUsed, limit);
        final String blackduckDbPath = commonRepositoryTaskHelper.getBlackduckPanelPath(AssetPanelLabel.TASK_STATUS);
        pagedQueryBuilder.and(blackduckDbPath).eq(TaskStatus.SUCCESS);
        return pagedQueryBuilder.build();
    }

    @Override
    protected boolean appliesTo(final Repository repository) {
        return commonRepositoryTaskHelper.doesRepositoryApply(repository, getRepositoryField());
    }

    @Override
    public String getMessage() {
        return commonRepositoryTaskHelper.getTaskMessage("Policy Check", getRepositoryField());
    }

}
