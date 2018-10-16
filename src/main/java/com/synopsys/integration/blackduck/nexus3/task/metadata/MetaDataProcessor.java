package com.synopsys.integration.blackduck.nexus3.task.metadata;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.view.VersionBomPolicyStatusView;
import com.synopsys.integration.blackduck.api.generated.view.VulnerabilityV2View;
import com.synopsys.integration.blackduck.api.generated.view.VulnerableComponentView;
import com.synopsys.integration.blackduck.nexus3.task.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.nexus3.util.AssetWrapper;
import com.synopsys.integration.blackduck.service.HubService;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.PolicyStatusDescription;
import com.synopsys.integration.exception.IntegrationException;

@Named
@Singleton
public class MetaDataProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;

    @Inject
    public MetaDataProcessor(final CommonRepositoryTaskHelper commonRepositoryTaskHelper) {
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
    }

    public void updateAssetVulnerabilityData(final VulnerabilityLevels vulnerabilityLevels, final AssetWrapper assetWrapper) {
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.HIGH_VULNERABILITY, String.valueOf(vulnerabilityLevels.getHighVulnerabilityCount()));
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.MEDIUM_VULNERABILITY, String.valueOf(vulnerabilityLevels.getMediumVulnerabilityCount()));
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.LOW_VULNERABILITY, String.valueOf(vulnerabilityLevels.getLowVulnerabilityCount()));
        assetWrapper.updateAsset();
    }

    public List<VulnerableComponentView> checkAssetVulnerabilities(final String name, final String version) throws IntegrationException {
        final HubServicesFactory hubServicesFactory = commonRepositoryTaskHelper.getHubServicesFactory();
        final ProjectService projectService = hubServicesFactory.createProjectService();
        return projectService.getVulnerableComponentsForProjectVersion(name, version);
    }

    public List<VulnerabilityV2View> convertToVulnerabilities(final VulnerableComponentView vulnerableComponentView) throws IntegrationException {
        final HubServicesFactory hubServicesFactory = commonRepositoryTaskHelper.getHubServicesFactory();
        final HubService hubService = hubServicesFactory.createHubService();
        return hubService.getAllResponses(vulnerableComponentView, VulnerableComponentView.VULNERABILITIES_LINK_RESPONSE);
    }

    public void removeAssetVulnerabilityData(final AssetWrapper assetWrapper) {
        assetWrapper.removeFromBlackDuckAssetPanel(AssetPanelLabel.HIGH_VULNERABILITY);
        assetWrapper.removeFromBlackDuckAssetPanel(AssetPanelLabel.MEDIUM_VULNERABILITY);
        assetWrapper.removeFromBlackDuckAssetPanel(AssetPanelLabel.LOW_VULNERABILITY);
        assetWrapper.updateAsset();
    }

    public void updateAssetPolicyData(final VersionBomPolicyStatusView policyStatusView, final AssetWrapper assetWrapper) {
        final PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(policyStatusView);
        final String policyStatus = policyStatusDescription.getPolicyStatusMessage();
        final String overallStatus = policyStatusView.overallStatus.prettyPrint();

        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.POLICY_STATUS, policyStatus);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS, overallStatus);
        assetWrapper.updateAsset();
    }

    public void removePolicyData(final AssetWrapper assetWrapper) {
        assetWrapper.removeFromBlackDuckAssetPanel(AssetPanelLabel.POLICY_STATUS);
        assetWrapper.removeFromBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS);
        assetWrapper.updateAsset();
    }

    public VersionBomPolicyStatusView checkAssetPolicy(final String name, final String version) throws IntegrationException {
        logger.info("Checking metadata of {}", name);
        final HubServicesFactory hubServicesFactory = commonRepositoryTaskHelper.getHubServicesFactory();
        final ProjectService projectService = hubServicesFactory.createProjectService();
        return projectService.getPolicyStatusForProjectAndVersion(name, version);
    }

}
