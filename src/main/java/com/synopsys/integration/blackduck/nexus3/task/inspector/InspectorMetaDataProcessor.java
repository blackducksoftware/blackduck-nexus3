package com.synopsys.integration.blackduck.nexus3.task.inspector;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.component.RiskCountView;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonMetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.metadata.VulnerabilityLevels;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;

@Named
@Singleton
public class InspectorMetaDataProcessor {
    public static final String PROXY_INSPECTION_VERSION = "Nexus-3-Plugin";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final CommonMetaDataProcessor commonMetaDataProcessor;
    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;

    @Inject
    public InspectorMetaDataProcessor(final CommonMetaDataProcessor commonMetaDataProcessor, final CommonRepositoryTaskHelper commonRepositoryTaskHelper) {
        this.commonMetaDataProcessor = commonMetaDataProcessor;
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
    }

    public ProjectVersionWrapper getProjectVersionWrapper(final String name) throws IntegrationException {
        return commonRepositoryTaskHelper.getProjectVersionWrapper(name, PROXY_INSPECTION_VERSION);
    }

    public void updateRepositoryMetaData(final ProjectVersionView projectVersionView, final Map<String, AssetWrapper> assetWrapperMap, final TaskStatus status) throws IntegrationException {
        final List<VersionBomComponentView> versionBomComponentViews = commonMetaDataProcessor.checkAssetVulnerabilities(projectVersionView);
        for (final VersionBomComponentView versionBomComponentView : versionBomComponentViews) {
            final Set<String> externalIds = versionBomComponentView.origins.stream()
                                                .map(versionBomOriginView -> versionBomOriginView.externalId)
                                                .collect(Collectors.toSet());
            logger.debug("Found all externalIds ({}) for component: {}", externalIds, versionBomComponentView.componentName);
            final Optional<AssetWrapper> assetWrapperOptional = findAssetWrapper(assetWrapperMap, externalIds);
            if (assetWrapperOptional.isPresent()) {
                final AssetWrapper assetWrapper = assetWrapperOptional.get();

                final String componentUrl = versionBomComponentView.componentVersion;
                final PolicySummaryStatusType policyStatus = versionBomComponentView.policyStatus;

                logger.info("Found component and updating Asset: {}", assetWrapper.getName());
                assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_STATUS, status.name());
                assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.BLACKDUCK_URL, componentUrl);
                assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS, policyStatus.prettyPrint());
                addVulnerabilityStatus(assetWrapper, versionBomComponentView);
                assetWrapper.updateAsset();
            }
        }
    }

    private void addVulnerabilityStatus(final AssetWrapper assetWrapper, final VersionBomComponentView versionBomComponentView) {
        final VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();
        final List<RiskCountView> riskCountViews = versionBomComponentView.securityRiskProfile.counts;
        logger.info("Counting vulnerabilities");
        commonMetaDataProcessor.addAllAssetVulnerabilityCounts(riskCountViews, vulnerabilityLevels);
        commonMetaDataProcessor.setAssetVulnerabilityData(vulnerabilityLevels, assetWrapper);
    }

    private Optional<AssetWrapper> findAssetWrapper(final Map<String, AssetWrapper> assetWrapperMap, final Set<String> externalIds) {
        return externalIds.stream()
                   .filter(externalId -> assetWrapperMap.containsKey(externalId))
                   .map(externalId -> assetWrapperMap.get(externalId))
                   .findFirst();
    }
}
