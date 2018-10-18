package com.synopsys.integration.blackduck.nexus3.task.metadata;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.component.RiskCountView;
import com.synopsys.integration.blackduck.api.generated.enumeration.RiskCountType;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomPolicyStatusView;
import com.synopsys.integration.blackduck.nexus3.task.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.nexus3.util.AssetWrapper;
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

    public List<VersionBomComponentView> checkAssetVulnerabilities(final String name, final String version) throws IntegrationException {
        final HubServicesFactory hubServicesFactory = commonRepositoryTaskHelper.getHubServicesFactory();
        final ProjectService projectService = hubServicesFactory.createProjectService();
        return projectService.getComponentsForProjectVersion(name, version);
    }

    public List<VersionBomComponentView> checkAssetVulnerabilities(final ProjectVersionView projectVersionView) throws IntegrationException {
        final HubServicesFactory hubServicesFactory = commonRepositoryTaskHelper.getHubServicesFactory();
        final ProjectService projectService = hubServicesFactory.createProjectService();
        return projectService.getComponentsForProjectVersion(projectVersionView);
    }

    public void addAllAssetVulnerabilityCounts(final List<RiskCountView> vulnerabilities, final VulnerabilityLevels vulnerabilityLevels) {
        for (final RiskCountView riskCountView : vulnerabilities) {
            final String riskCountType = riskCountView.countType.name();
            final int riskCount = riskCountView.count;
            vulnerabilityLevels.addXVulnerabilities(riskCountType, riskCount);
        }
    }

    public void addMaxAssetVulnerabilityCounts(final List<RiskCountView> vulnerabilities, final VulnerabilityLevels vulnerabilityLevels) {
        String highestSeverity = "";
        for (final RiskCountView vulnerability : vulnerabilities) {
            final RiskCountType severity = vulnerability.countType;
            final int severityCount = vulnerability.count;
            if (RiskCountType.HIGH.equals(severity) && severityCount > 0) {
                highestSeverity = VulnerabilityLevels.HIGH_VULNERABILITY;
                break;
            } else if (RiskCountType.MEDIUM.equals(severity) && severityCount > 0) {
                highestSeverity = VulnerabilityLevels.MEDIUM_VULNERABILITY;
            } else if (RiskCountType.LOW.equals(severity) && severityCount > 0) {
                highestSeverity = VulnerabilityLevels.LOW_VULNERABILITY;
            }
        }
        if (StringUtils.isNotBlank(highestSeverity)) {
            vulnerabilityLevels.addVulnerability(highestSeverity);
        }
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
