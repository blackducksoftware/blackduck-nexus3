package com.synopsys.integration.blackduck.nexus3.task.common;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.synopsys.integration.blackduck.api.generated.component.ComponentVersionRiskProfileRiskDataCountsView;
import com.synopsys.integration.blackduck.api.generated.enumeration.ComponentVersionRiskProfileRiskDataCountsCountTypeType;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyStatusType;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionPolicyStatusView;
import com.synopsys.integration.blackduck.api.manual.throwaway.generated.component.NameValuePairView;
import com.synopsys.integration.blackduck.nexus3.mock.model.MockAsset;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;

public class CommonMetaDataProcessorTest {

    @Test
    public void setAssetVulnerabilityDataTest() {
        CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();
        MockAsset mockAsset = new MockAsset();
        AssetWrapper assetWrapper = AssetWrapper.createScanAssetWrapper(mockAsset, null, null);

        String vulnerabilitiesContent = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.VULNERABILITIES);
        Assert.assertTrue(StringUtils.isBlank(vulnerabilitiesContent));

        VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();
        BigDecimal expectedHigh = new BigDecimal(3);
        BigDecimal expectedLow = new BigDecimal(1);
        vulnerabilityLevels.addXVulnerabilities(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH, expectedHigh);
        vulnerabilityLevels.addXVulnerabilities(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW, expectedLow);
        commonMetaDataProcessor.setAssetVulnerabilityData(vulnerabilityLevels, assetWrapper);

        String vulnerabilityCounts = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.VULNERABILITIES);
        Map<String, Integer> vulnerabilityMapping = getVulnerabilityCounts(vulnerabilityCounts);

        int high = vulnerabilityMapping.get(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name());
        int medium = vulnerabilityMapping.get(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name());
        int low = vulnerabilityMapping.get(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name());

        Assert.assertEquals(expectedHigh.intValue(), high);
        Assert.assertEquals(0, medium);
        Assert.assertEquals(expectedLow.intValue(), low);
    }

    @Test
    public void addAllAssetVulnerabilityCountsTest() {
        CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();
        BigDecimal expectedHigh = new BigDecimal(3);
        BigDecimal expectedMedium = new BigDecimal(2);
        BigDecimal expectedLow = new BigDecimal(1);
        ComponentVersionRiskProfileRiskDataCountsView highRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        highRiskCountView.setCount(expectedHigh);
        highRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH);
        ComponentVersionRiskProfileRiskDataCountsView mediumRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        mediumRiskCountView.setCount(expectedMedium);
        mediumRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM);
        ComponentVersionRiskProfileRiskDataCountsView lowRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        lowRiskCountView.setCount(expectedLow);
        lowRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW);
        List<ComponentVersionRiskProfileRiskDataCountsView> riskCountViewList = Arrays.asList(highRiskCountView, mediumRiskCountView, lowRiskCountView);

        VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();

        int emptyHigh = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name(), 0);
        int emptyMedium = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name(), 0);
        int emptyLow = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name(), 0);

        Assert.assertEquals(0, emptyHigh);
        Assert.assertEquals(0, emptyMedium);
        Assert.assertEquals(0, emptyLow);

        commonMetaDataProcessor.addAllAssetVulnerabilityCounts(riskCountViewList, vulnerabilityLevels);

        int high = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name(), 0);
        int medium = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name(), 0);
        int low = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name(), 0);

        Assert.assertEquals(expectedHigh.intValue(), high);
        Assert.assertEquals(expectedMedium.intValue(), medium);
        Assert.assertEquals(expectedLow.intValue(), low);
    }

    @Test
    public void addMaxAssetVulnerabilityCountsCriticalTest() {
        CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();
        BigDecimal expectedCritical = new BigDecimal(1);
        BigDecimal expectedHigh = new BigDecimal(3);
        BigDecimal expectedMedium = new BigDecimal(2);
        BigDecimal expectedLow = new BigDecimal(1);
        ComponentVersionRiskProfileRiskDataCountsView criticalRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        criticalRiskCountView.setCount(expectedCritical);
        criticalRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.CRITICAL);
        ComponentVersionRiskProfileRiskDataCountsView highRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        highRiskCountView.setCount(expectedHigh);
        highRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH);
        ComponentVersionRiskProfileRiskDataCountsView mediumRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        mediumRiskCountView.setCount(expectedMedium);
        mediumRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM);
        ComponentVersionRiskProfileRiskDataCountsView lowRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        lowRiskCountView.setCount(expectedLow);
        lowRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW);
        List<ComponentVersionRiskProfileRiskDataCountsView> riskCountViewList = Arrays.asList(criticalRiskCountView, highRiskCountView, mediumRiskCountView, lowRiskCountView);

        VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();

        int emptyCritical = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.CRITICAL.name(), 0);
        int emptyHigh = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name(), 0);
        int emptyMedium = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name(), 0);
        int emptyLow = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name(), 0);

        Assert.assertEquals(0, emptyCritical);
        Assert.assertEquals(0, emptyHigh);
        Assert.assertEquals(0, emptyMedium);
        Assert.assertEquals(0, emptyLow);

        commonMetaDataProcessor.addMaxAssetVulnerabilityCounts(riskCountViewList, vulnerabilityLevels);

        int critical = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.CRITICAL.name(), 0);
        int high = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name(), 0);
        int medium = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name(), 0);
        int low = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name(), 0);

        Assert.assertEquals(1, critical);
        Assert.assertEquals(0, high);
        Assert.assertEquals(0, medium);
        Assert.assertEquals(0, low);
    }

    @Test
    public void addMaxAssetVulnerabilityCountsHighTest() {
        CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();
        BigDecimal expectedHigh = new BigDecimal(3);
        BigDecimal expectedMedium = new BigDecimal(2);
        BigDecimal expectedLow = new BigDecimal(1);
        ComponentVersionRiskProfileRiskDataCountsView highRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        highRiskCountView.setCount(expectedHigh);
        highRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH);
        ComponentVersionRiskProfileRiskDataCountsView mediumRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        mediumRiskCountView.setCount(expectedMedium);
        mediumRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM);
        ComponentVersionRiskProfileRiskDataCountsView lowRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        lowRiskCountView.setCount(expectedLow);
        lowRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW);
        List<ComponentVersionRiskProfileRiskDataCountsView> riskCountViewList = Arrays.asList(highRiskCountView, mediumRiskCountView, lowRiskCountView);

        VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();

        int emptyHigh = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name(), 0);
        int emptyMedium = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name(), 0);
        int emptyLow = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name(), 0);

        Assert.assertEquals(0, emptyHigh);
        Assert.assertEquals(0, emptyMedium);
        Assert.assertEquals(0, emptyLow);

        commonMetaDataProcessor.addMaxAssetVulnerabilityCounts(riskCountViewList, vulnerabilityLevels);

        int high = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name(), 0);
        int medium = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name(), 0);
        int low = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name(), 0);

        Assert.assertEquals(1, high);
        Assert.assertEquals(0, medium);
        Assert.assertEquals(0, low);
    }

    @Test
    public void addMaxAssetVulnerabilityCountsLowTest() {
        CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();
        BigDecimal expectedCritical = new BigDecimal(0);
        BigDecimal expectedHigh = new BigDecimal(0);
        BigDecimal expectedMedium = new BigDecimal(0);
        BigDecimal expectedLow = new BigDecimal(1);
        ComponentVersionRiskProfileRiskDataCountsView criticalRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        criticalRiskCountView.setCount(expectedCritical);
        criticalRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.CRITICAL);
        ComponentVersionRiskProfileRiskDataCountsView highRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        highRiskCountView.setCount(expectedHigh);
        highRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH);
        ComponentVersionRiskProfileRiskDataCountsView mediumRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        mediumRiskCountView.setCount(expectedMedium);
        mediumRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM);
        ComponentVersionRiskProfileRiskDataCountsView lowRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        lowRiskCountView.setCount(expectedLow);
        lowRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW);
        List<ComponentVersionRiskProfileRiskDataCountsView> riskCountViewList = Arrays.asList(criticalRiskCountView, highRiskCountView, mediumRiskCountView, lowRiskCountView);

        VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();

        int emptyCritical = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.CRITICAL.name(), 0);
        int emptyHigh = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name(), 0);
        int emptyMedium = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name(), 0);
        int emptyLow = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name(), 0);

        Assert.assertEquals(0, emptyCritical);
        Assert.assertEquals(0, emptyHigh);
        Assert.assertEquals(0, emptyMedium);
        Assert.assertEquals(0, emptyLow);

        commonMetaDataProcessor.addMaxAssetVulnerabilityCounts(riskCountViewList, vulnerabilityLevels);

        int critical = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.CRITICAL.name(), 0);
        int high = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name(), 0);
        int medium = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name(), 0);
        int low = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name(), 0);

        Assert.assertEquals(0, critical);
        Assert.assertEquals(0, high);
        Assert.assertEquals(0, medium);
        Assert.assertEquals(1, low);
    }

    @Test
    public void addMaxAssetVulnerabilityCountsNoVulnerabilitiesTest() {
        CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();
        BigDecimal expectedCritical = new BigDecimal(0);
        BigDecimal expectedHigh = new BigDecimal(0);
        BigDecimal expectedMedium = new BigDecimal(0);
        BigDecimal expectedLow = new BigDecimal(0);
        ComponentVersionRiskProfileRiskDataCountsView criticalRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        criticalRiskCountView.setCount(expectedCritical);
        criticalRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.CRITICAL);
        ComponentVersionRiskProfileRiskDataCountsView highRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        highRiskCountView.setCount(expectedHigh);
        highRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH);
        ComponentVersionRiskProfileRiskDataCountsView mediumRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        mediumRiskCountView.setCount(expectedMedium);
        mediumRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM);
        ComponentVersionRiskProfileRiskDataCountsView lowRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        lowRiskCountView.setCount(expectedLow);
        lowRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW);
        List<ComponentVersionRiskProfileRiskDataCountsView> riskCountViewList = Arrays.asList(criticalRiskCountView, highRiskCountView, mediumRiskCountView, lowRiskCountView);

        VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();

        int emptyCritical = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.CRITICAL.name(), 0);
        int emptyHigh = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name(), 0);
        int emptyMedium = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name(), 0);
        int emptyLow = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name(), 0);

        Assert.assertEquals(0, emptyCritical);
        Assert.assertEquals(0, emptyHigh);
        Assert.assertEquals(0, emptyMedium);
        Assert.assertEquals(0, emptyLow);

        commonMetaDataProcessor.addMaxAssetVulnerabilityCounts(riskCountViewList, vulnerabilityLevels);

        int critical = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.CRITICAL.name(), 0);
        int high = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name(), 0);
        int medium = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name(), 0);
        int low = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name(), 0);

        Assert.assertEquals(0, critical);
        Assert.assertEquals(0, high);
        Assert.assertEquals(0, medium);
        Assert.assertEquals(0, low);
    }

    @Test
    public void addMaxAssetVulnerabilityCountsNoneTest() {
        CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();

        List<ComponentVersionRiskProfileRiskDataCountsView> riskCountViewList = Arrays.asList();

        VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();

        int emptyCritical = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.CRITICAL.name(), 0);
        int emptyHigh = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name(), 0);
        int emptyMedium = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name(), 0);
        int emptyLow = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name(), 0);

        Assert.assertEquals(0, emptyCritical);
        Assert.assertEquals(0, emptyHigh);
        Assert.assertEquals(0, emptyMedium);
        Assert.assertEquals(0, emptyLow);

        commonMetaDataProcessor.addMaxAssetVulnerabilityCounts(riskCountViewList, vulnerabilityLevels);

        int critical = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.CRITICAL.name(), 0);
        int high = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name(), 0);
        int medium = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name(), 0);
        int low = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name(), 0);

        Assert.assertEquals(0, critical);
        Assert.assertEquals(0, high);
        Assert.assertEquals(0, medium);
        Assert.assertEquals(0, low);
    }

    @Test
    public void addMaxAssetVulnerabilityCountsNullTest() {
        CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();

        List<ComponentVersionRiskProfileRiskDataCountsView> riskCountViewList = Arrays.asList(null, null);

        VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();

        int emptyCritical = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.CRITICAL.name(), 0);
        int emptyHigh = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name(), 0);
        int emptyMedium = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name(), 0);
        int emptyLow = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name(), 0);

        Assert.assertEquals(0, emptyCritical);
        Assert.assertEquals(0, emptyHigh);
        Assert.assertEquals(0, emptyMedium);
        Assert.assertEquals(0, emptyLow);

        commonMetaDataProcessor.addMaxAssetVulnerabilityCounts(riskCountViewList, vulnerabilityLevels);

        int critical = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.CRITICAL.name(), 0);
        int high = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name(), 0);
        int medium = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name(), 0);
        int low = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name(), 0);

        Assert.assertEquals(0, critical);
        Assert.assertEquals(0, high);
        Assert.assertEquals(0, medium);
        Assert.assertEquals(0, low);
    }

    @Test
    public void addMaxAssetVulnerabilityCountsMissingCountTest() {
        CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();
        BigDecimal expectedMedium = new BigDecimal(1);
        BigDecimal expectedMissingType = new BigDecimal(1);
        ComponentVersionRiskProfileRiskDataCountsView criticalRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        criticalRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.CRITICAL);
        ComponentVersionRiskProfileRiskDataCountsView mediumRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        mediumRiskCountView.setCount(expectedMedium);
        mediumRiskCountView.setCountType(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM);
        ComponentVersionRiskProfileRiskDataCountsView missingTypeRiskCountView = new ComponentVersionRiskProfileRiskDataCountsView();
        missingTypeRiskCountView.setCount(expectedMissingType);
        List<ComponentVersionRiskProfileRiskDataCountsView> riskCountViewList = Arrays.asList(criticalRiskCountView, mediumRiskCountView, missingTypeRiskCountView);

        VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();

        int emptyCritical = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.CRITICAL.name(), 0);
        int emptyHigh = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name(), 0);
        int emptyMedium = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name(), 0);
        int emptyLow = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name(), 0);

        Assert.assertEquals(0, emptyCritical);
        Assert.assertEquals(0, emptyHigh);
        Assert.assertEquals(0, emptyMedium);
        Assert.assertEquals(0, emptyLow);

        commonMetaDataProcessor.addMaxAssetVulnerabilityCounts(riskCountViewList, vulnerabilityLevels);

        int critical = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.CRITICAL.name(), 0);
        int high = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name(), 0);
        int medium = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name(), 0);
        int low = vulnerabilityLevels.getVulnerabilityCount(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name(), 0);

        Assert.assertEquals(0, critical);
        Assert.assertEquals(0, high);
        Assert.assertEquals(1, medium);
        Assert.assertEquals(0, low);
    }

    @Test
    public void removeAssetVulnerabilityDataTest() {
        CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();
        VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();
        vulnerabilityLevels.addXVulnerabilities(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW, new BigDecimal(4));

        MockAsset mockAsset = new MockAsset();
        AssetWrapper assetWrapper = AssetWrapper.createScanAssetWrapper(mockAsset, null, null);
        commonMetaDataProcessor.setAssetVulnerabilityData(vulnerabilityLevels, assetWrapper);

        String vulnerabilityContent = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.VULNERABILITIES);
        Assert.assertTrue(StringUtils.isNotBlank(vulnerabilityContent));

        commonMetaDataProcessor.removeAssetVulnerabilityData(assetWrapper);

        String vulnerabilityCounts = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.VULNERABILITIES);
        Map<String, Integer> vulnerabilityMapping = getVulnerabilityCounts(vulnerabilityCounts);

        Assert.assertTrue(vulnerabilityMapping.isEmpty());
    }

    @Test
    public void setAssetPolicyDataTest() {
        CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();
        MockAsset mockAsset = new MockAsset();
        AssetWrapper assetWrapper = AssetWrapper.createScanAssetWrapper(mockAsset, null, null);

        String policyStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.POLICY_STATUS);
        String policyOverallStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS);
        Assert.assertTrue(StringUtils.isBlank(policyStatus));
        Assert.assertTrue(StringUtils.isBlank(policyOverallStatus));

        ProjectVersionPolicyStatusView versionBomPolicyStatusView = createVersionBomPolicyStatusView();

        commonMetaDataProcessor.setAssetPolicyData(versionBomPolicyStatusView, assetWrapper);

        String foundPolicyStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.POLICY_STATUS);
        String foundOverallPolicyStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS);

        String notInViolation = PolicySummaryStatusType.NOT_IN_VIOLATION.prettyPrint();
        final String notInViolationText = "Black Duck found: 0 components in violation, 0 components in violation, but overridden, and 5 components not in violation.";

        Assert.assertEquals(notInViolation, foundOverallPolicyStatus);
        Assert.assertEquals(notInViolationText, foundPolicyStatus);
    }

    @Test
    public void removePolicyDataTest() {
        CommonMetaDataProcessor commonMetaDataProcessor = new CommonMetaDataProcessor();
        MockAsset mockAsset = new MockAsset();
        AssetWrapper assetWrapper = AssetWrapper.createScanAssetWrapper(mockAsset, null, null);

        ProjectVersionPolicyStatusView versionBomPolicyStatusView = createVersionBomPolicyStatusView();
        commonMetaDataProcessor.setAssetPolicyData(versionBomPolicyStatusView, assetWrapper);

        String foundPolicyStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.POLICY_STATUS);
        String foundOverallPolicyStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS);

        Assert.assertTrue(StringUtils.isNotBlank(foundPolicyStatus));
        Assert.assertTrue(StringUtils.isNotBlank(foundOverallPolicyStatus));

        commonMetaDataProcessor.removePolicyData(assetWrapper);

        String notFoundPolicyStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.POLICY_STATUS);
        String notFoundOverallPolicyStatus = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS);

        Assert.assertTrue(StringUtils.isBlank(notFoundPolicyStatus));
        Assert.assertTrue(StringUtils.isBlank(notFoundOverallPolicyStatus));
    }

    private Map<String, Integer> getVulnerabilityCounts(String vulnerabilityCounts) {
        if (StringUtils.isBlank(vulnerabilityCounts)) {
            return Collections.emptyMap();
        }

        Map<String, Integer> counts = new HashMap<>();
        String[] vulnerabilityItems = vulnerabilityCounts.split(",");
        for (String vulnerabilityItem : vulnerabilityItems) {
            String[] number = vulnerabilityItem.trim().split(" ");
            int vulnNumber = 0;
            try {
                vulnNumber = Integer.parseInt(number[0]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            if (vulnerabilityItem.contains("High")) {
                counts.put(ComponentVersionRiskProfileRiskDataCountsCountTypeType.HIGH.name(), vulnNumber);
            } else if (vulnerabilityItem.contains("Medium")) {
                counts.put(ComponentVersionRiskProfileRiskDataCountsCountTypeType.MEDIUM.name(), vulnNumber);
            } else if (vulnerabilityItem.contains("Low")) {
                counts.put(ComponentVersionRiskProfileRiskDataCountsCountTypeType.LOW.name(), vulnNumber);
            }
        }
        return counts;
    }

    private ProjectVersionPolicyStatusView createVersionBomPolicyStatusView() {
        ProjectVersionPolicyStatusView versionBomPolicyStatusView = new ProjectVersionPolicyStatusView();
        versionBomPolicyStatusView.setOverallStatus(PolicyStatusType.NOT_IN_VIOLATION);

        NameValuePairView notInViolation = new NameValuePairView();
        notInViolation.setName(PolicyStatusType.NOT_IN_VIOLATION.name());
        notInViolation.setValue(5);
        NameValuePairView inViolation = new NameValuePairView();
        inViolation.setName(PolicyStatusType.IN_VIOLATION.name());
        inViolation.setValue(0);
        NameValuePairView inViolationButOverridden = new NameValuePairView();
        inViolationButOverridden.setName(PolicyStatusType.IN_VIOLATION_OVERRIDDEN.name());
        inViolationButOverridden.setValue(0);
        versionBomPolicyStatusView.setComponentVersionStatusCounts(Arrays.asList(notInViolation, inViolation, inViolationButOverridden));

        return versionBomPolicyStatusView;
    }

}
