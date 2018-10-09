package com.synopsys.integration.blackduck.nexus3.task.policy;

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
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanel;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.nexus3.util.AssetWrapper;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.PolicyStatusDescription;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

@Named
public class PolicyCheckTask extends RepositoryTaskSupport {
    public static final int PAGED_SIZE_LIMIT = 100;

    private final Logger logger = createLogger();
    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final QueryManager queryManager;

    @Inject
    public PolicyCheckTask(final CommonRepositoryTaskHelper commonRepositoryTaskHelper, final QueryManager queryManager) {
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.queryManager = queryManager;
    }

    @Override
    protected void execute(final Repository repository) {
        final HubServerConfig hubServerConfig = commonRepositoryTaskHelper.getHubServerConfig();
        final Query filteredAssets = createFilteredQuery(Optional.empty(), PAGED_SIZE_LIMIT);
        PagedResult<Asset> pagedAssets = commonRepositoryTaskHelper.pagedAssets(repository, filteredAssets);
        while (pagedAssets.hasResults()) {
            for (final Asset asset : pagedAssets.getTypeList()) {
                logger.debug("All associated attributes: {}", asset.attributes());
                logger.debug("All associated BD attribuets: {}", asset.attributes().child(AssetPanel.BLACKDUCK_CATEGORY));
                final AssetWrapper assetWrapper = new AssetWrapper(asset, repository, queryManager);
                final String name = assetWrapper.getName();
                final String version = assetWrapper.getVersion();
                try {
                    logger.info("Starting to check policies");
                    final VersionBomPolicyStatusView policyStatusView = createPolicyChecker(hubServerConfig, name, version);
                    final PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(policyStatusView);
                    final String policyStatus = policyStatusDescription.getPolicyStatusMessage();
                    final String overallStatus = policyStatusView.overallStatus.prettyPrint();

                    assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.POLICY_STATUS, policyStatus);
                    assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS, overallStatus);
                    assetWrapper.updateAsset();
                } catch (final IntegrationException e) {
                    throw new TaskInterruptedException("Problem checking policy: " + e.getMessage(), true);
                }
            }

            final Query.Builder nextPage = commonRepositoryTaskHelper.createPagedQuery(pagedAssets.getLastName(), PAGED_SIZE_LIMIT);
            pagedAssets = commonRepositoryTaskHelper.pagedAssets(repository, nextPage.build());
        }
    }

    private Query createFilteredQuery(final Optional<String> lastNameUsed, final int limit) {
        final Query.Builder pagedQueryBuilder = commonRepositoryTaskHelper.createPagedQuery(lastNameUsed, limit);
        final String blackduckDbPath = commonRepositoryTaskHelper.getBlackduckPanelPath(AssetPanelLabel.TASK_STATUS);
        pagedQueryBuilder.and(blackduckDbPath).eq(TaskStatus.SUCCESS);
        return pagedQueryBuilder.build();
    }

    private VersionBomPolicyStatusView createPolicyChecker(final HubServerConfig hubServerConfig, final String projectName, final String projectVersion) throws IntegrationException {
        final IntLogger intLogger = new Slf4jIntLogger(logger);
        final HubServicesFactory hubServicesFactory = new HubServicesFactory(HubServicesFactory.createDefaultGson(), HubServicesFactory.createDefaultJsonParser(), hubServerConfig.createRestConnection(intLogger), intLogger);
        final ProjectService projectService = hubServicesFactory.createProjectService();
        return projectService.getPolicyStatusForProjectAndVersion(projectName, projectVersion);
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
