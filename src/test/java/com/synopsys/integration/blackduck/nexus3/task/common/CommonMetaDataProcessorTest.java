package com.synopsys.integration.blackduck.nexus3.task.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.synopsys.integration.blackduck.api.generated.component.NameValuePairView;
import com.synopsys.integration.blackduck.api.generated.component.RiskCountView;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.api.generated.enumeration.RiskCountType;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomPolicyStatusView;
import com.synopsys.integration.blackduck.nexus3.mock.model.MockAsset;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;

public class CommonMetaDataProcessorTest {

    @Test
    public void setAssetVulnerabilityDataTest() {
        final CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();
        final MockAsset mockAsset = new MockAsset();
        final AssetWrapper assetWrapper = new AssetWrapper(mockAsset, null, null);

        final String vulnerabilitiesContent = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.VULNERABILITIES);
        Assert.assertTrue(StringUtils.isBlank(vulnerabilitiesContent));

        final VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();
        final int expectedHigh = 3;
        final int expectedLow = 1;
        vulnerabilityLevels.addXVulnerabilities(VulnerabilityLevels.HIGH_VULNERABILITY, expectedHigh);
        vulnerabilityLevels.addXVulnerabilities(VulnerabilityLevels.LOW_VULNERABILITY, expectedLow);
        commonMetaDataProcessor.setAssetVulnerabilityData(vulnerabilityLevels, assetWrapper);

        final String vulnerabilityCounts = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.VULNERABILITIES);
        final Map<String, Integer> vulnerabilityMapping = getVulnerabilityCounts(vulnerabilityCounts);

        final int high = vulnerabilityMapping.get(VulnerabilityLevels.HIGH_VULNERABILITY);
        final int medium = vulnerabilityMapping.get(VulnerabilityLevels.MEDIUM_VULNERABILITY);
        final int low = vulnerabilityMapping.get(VulnerabilityLevels.LOW_VULNERABILITY);

        Assert.assertEquals(expectedHigh, high);
        Assert.assertEquals(0, medium);
        Assert.assertEquals(expectedLow, low);
    }

    @Test
    public void addAllAssetVulnerabilityCountsTest() {
        final CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();
        final int expectedHigh = 3;
        final int expectedMedium = 2;
        final int expectedLow = 1;
        final RiskCountView highRiskCountView = new RiskCountView();
        highRiskCountView.count = expectedHigh;
        highRiskCountView.countType = RiskCountType.HIGH;
        final RiskCountView mediumRiskCountView = new RiskCountView();
        mediumRiskCountView.count = expectedMedium;
        mediumRiskCountView.countType = RiskCountType.MEDIUM;
        final RiskCountView lowRiskCountView = new RiskCountView();
        lowRiskCountView.count = expectedLow;
        lowRiskCountView.countType = RiskCountType.LOW;
        final List<RiskCountView> riskCountViewList = Arrays.asList(highRiskCountView, mediumRiskCountView, lowRiskCountView);

        final VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();

        final int emptyHigh = vulnerabilityLevels.getVulnerabilityCount(VulnerabilityLevels.HIGH_VULNERABILITY, 0);
        final int emptyMedium = vulnerabilityLevels.getVulnerabilityCount(VulnerabilityLevels.MEDIUM_VULNERABILITY, 0);
        final int emptyLow = vulnerabilityLevels.getVulnerabilityCount(VulnerabilityLevels.LOW_VULNERABILITY, 0);

        Assert.assertEquals(0, emptyHigh);
        Assert.assertEquals(0, emptyMedium);
        Assert.assertEquals(0, emptyLow);

        commonMetaDataProcessor.addAllAssetVulnerabilityCounts(riskCountViewList, vulnerabilityLevels);

        final int high = vulnerabilityLevels.getVulnerabilityCount(VulnerabilityLevels.HIGH_VULNERABILITY, 0);
        final int medium = vulnerabilityLevels.getVulnerabilityCount(VulnerabilityLevels.MEDIUM_VULNERABILITY, 0);
        final int low = vulnerabilityLevels.getVulnerabilityCount(VulnerabilityLevels.LOW_VULNERABILITY, 0);

        Assert.assertEquals(expectedHigh, high);
        Assert.assertEquals(expectedMedium, medium);
        Assert.assertEquals(expectedLow, low);
    }

    @Test
    public void addMaxAssetVulnerabilityCountsTest() {
        final CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();
        final int expectedHigh = 3;
        final int expectedMedium = 2;
        final int expectedLow = 1;
        final RiskCountView highRiskCountView = new RiskCountView();
        highRiskCountView.count = expectedHigh;
        highRiskCountView.countType = RiskCountType.HIGH;
        final RiskCountView mediumRiskCountView = new RiskCountView();
        mediumRiskCountView.count = expectedMedium;
        mediumRiskCountView.countType = RiskCountType.MEDIUM;
        final RiskCountView lowRiskCountView = new RiskCountView();
        lowRiskCountView.count = expectedLow;
        lowRiskCountView.countType = RiskCountType.LOW;
        final List<RiskCountView> riskCountViewList = Arrays.asList(highRiskCountView, mediumRiskCountView, lowRiskCountView);

        final VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();

        final int emptyHigh = vulnerabilityLevels.getVulnerabilityCount(VulnerabilityLevels.HIGH_VULNERABILITY, 0);
        final int emptyMedium = vulnerabilityLevels.getVulnerabilityCount(VulnerabilityLevels.MEDIUM_VULNERABILITY, 0);
        final int emptyLow = vulnerabilityLevels.getVulnerabilityCount(VulnerabilityLevels.LOW_VULNERABILITY, 0);

        Assert.assertEquals(0, emptyHigh);
        Assert.assertEquals(0, emptyMedium);
        Assert.assertEquals(0, emptyLow);

        commonMetaDataProcessor.addMaxAssetVulnerabilityCounts(riskCountViewList, vulnerabilityLevels);

        final int high = vulnerabilityLevels.getVulnerabilityCount(VulnerabilityLevels.HIGH_VULNERABILITY, 0);
        final int medium = vulnerabilityLevels.getVulnerabilityCount(VulnerabilityLevels.MEDIUM_VULNERABILITY, 0);
        final int low = vulnerabilityLevels.getVulnerabilityCount(VulnerabilityLevels.LOW_VULNERABILITY, 0);

        Assert.assertEquals(1, high);
        Assert.assertEquals(0, medium);
        Assert.assertEquals(0, low);
    }

    @Test
    public void removeAssetVulnerabilityDataTest() {
        final CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();
        final VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();
        vulnerabilityLevels.addXVulnerabilities(VulnerabilityLevels.LOW_VULNERABILITY, 4);

        final MockAsset mockAsset = new MockAsset();
        final AssetWrapper assetWrapper = new AssetWrapper(mockAsset, null, null);
        commonMetaDataProcessor.setAssetVulnerabilityData(vulnerabilityLevels, assetWrapper);

        final String vulnerabilityContent = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.VULNERABILITIES);
        Assert.assertTrue(StringUtils.isNotBlank(vulnerabilityContent));

        commonMetaDataProcessor.removeAssetVulnerabilityData(assetWrapper);

        final String vulnerabilityCounts = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.VULNERABILITIES);
        final Map<String, Integer> vulnerabilityMapping = getVulnerabilityCounts(vulnerabilityCounts);

        Assert.assertTrue(vulnerabilityMapping.isEmpty());
    }

    @Test
    public void setAssetPolicyDataTest() {
        final CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();
        final MockAsset mockAsset = new MockAsset();
        final AssetWrapper assetWrapper = new AssetWrapper(mockAsset, null, null);

        final String policyStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.POLICY_STATUS);
        final String policyOverallStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS);
        Assert.assertTrue(StringUtils.isBlank(policyStatus));
        Assert.assertTrue(StringUtils.isBlank(policyOverallStatus));

        final VersionBomPolicyStatusView versionBomPolicyStatusView = createVersionBomPolicyStatusView();

        commonMetaDataProcessor.setAssetPolicyData(versionBomPolicyStatusView, assetWrapper);

        final String foundPolicyStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.POLICY_STATUS);
        final String foundOverallPolicyStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS);

        final String notInViolation = PolicySummaryStatusType.NOT_IN_VIOLATION.prettyPrint();
        final String notInViolationText = "The Hub found: 0 components in violation, 0 components in violation, but overridden, and 5 components not in violation.";

        Assert.assertEquals(notInViolation, foundOverallPolicyStatus);
        Assert.assertEquals(notInViolationText, foundPolicyStatus);
    }

    @Test
    public void removePolicyDataTest() {
        final CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();
        final MockAsset mockAsset = new MockAsset();
        final AssetWrapper assetWrapper = new AssetWrapper(mockAsset, null, null);

        final VersionBomPolicyStatusView versionBomPolicyStatusView = createVersionBomPolicyStatusView();
        commonMetaDataProcessor.setAssetPolicyData(versionBomPolicyStatusView, assetWrapper);

        final String foundPolicyStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.POLICY_STATUS);
        final String foundOverallPolicyStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS);

        Assert.assertTrue(StringUtils.isNotBlank(foundPolicyStatus));
        Assert.assertTrue(StringUtils.isNotBlank(foundOverallPolicyStatus));

        commonMetaDataProcessor.removePolicyData(assetWrapper);

        final String notFoundPolicyStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.POLICY_STATUS);
        final String notFoundOverallPolicyStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS);

        Assert.assertTrue(StringUtils.isBlank(notFoundPolicyStatus));
        Assert.assertTrue(StringUtils.isBlank(notFoundOverallPolicyStatus));
    }

    private Map<String, Integer> getVulnerabilityCounts(final String vulnerabilityCounts) {
        if (StringUtils.isBlank(vulnerabilityCounts)) {
            return Collections.emptyMap();
        }

        final Map<String, Integer> counts = new HashMap<>();
        final String[] vulnerabilityItems = vulnerabilityCounts.split(",");
        for (final String vulnerabilityItem : vulnerabilityItems) {
            final String[] number = vulnerabilityItem.trim().split(" ");
            int vulnNumber = 0;
            try {
                vulnNumber = Integer.parseInt(number[0]);
            } catch (final NumberFormatException e) {
                e.printStackTrace();
            }
            if (vulnerabilityItem.contains("High")) {
                counts.put(VulnerabilityLevels.HIGH_VULNERABILITY, vulnNumber);
            } else if (vulnerabilityItem.contains("Medium")) {
                counts.put(VulnerabilityLevels.MEDIUM_VULNERABILITY, vulnNumber);
            } else if (vulnerabilityItem.contains("Low")) {
                counts.put(VulnerabilityLevels.LOW_VULNERABILITY, vulnNumber);
            }
        }
        return counts;
    }

    private VersionBomPolicyStatusView createVersionBomPolicyStatusView() {
        final VersionBomPolicyStatusView versionBomPolicyStatusView = new VersionBomPolicyStatusView();
        versionBomPolicyStatusView.overallStatus = PolicySummaryStatusType.NOT_IN_VIOLATION;

        final NameValuePairView notInViolation = new NameValuePairView();
        notInViolation.name = PolicySummaryStatusType.NOT_IN_VIOLATION.name();
        notInViolation.value = 5;
        final NameValuePairView inViolation = new NameValuePairView();
        inViolation.name = PolicySummaryStatusType.IN_VIOLATION.name();
        inViolation.value = 0;
        final NameValuePairView inViolationButOverridden = new NameValuePairView();
        inViolationButOverridden.name = PolicySummaryStatusType.IN_VIOLATION_OVERRIDDEN.name();
        inViolationButOverridden.value = 0;
        versionBomPolicyStatusView.componentVersionStatusCounts = Arrays.asList(notInViolation, inViolation, inViolationButOverridden);

        return versionBomPolicyStatusView;
    }

}
