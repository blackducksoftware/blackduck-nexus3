/**
 * blackduck-nexus3
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.nexus3.task.common;

import java.util.List;
import java.util.Optional;

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
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.PolicyStatusDescription;
import com.synopsys.integration.exception.IntegrationException;

@Named
@Singleton
public class CommonMetaDataProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void setAssetVulnerabilityData(final VulnerabilityLevels vulnerabilityLevels, final AssetWrapper assetWrapper) {
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.VULNERABILITIES, vulnerabilityLevels.getAllCounts());
    }

    public List<VersionBomComponentView> checkAssetVulnerabilities(final BlackDuckServicesFactory blackDuckServicesFactory, final ProjectVersionView projectVersionView) throws IntegrationException {
        final ProjectService projectService = blackDuckServicesFactory.createProjectService();
        return projectService.getComponentsForProjectVersion(projectVersionView);
    }

    public void addAllAssetVulnerabilityCounts(final List<RiskCountView> vulnerabilities, final VulnerabilityLevels vulnerabilityLevels) {
        for (final RiskCountView riskCountView : vulnerabilities) {
            final String riskCountType = riskCountView.getCountType().name();
            final int riskCount = riskCountView.getCount();
            vulnerabilityLevels.addXVulnerabilities(riskCountType, riskCount);
        }
    }

    public void addMaxAssetVulnerabilityCounts(final List<RiskCountView> vulnerabilities, final VulnerabilityLevels vulnerabilityLevels) {
        String highestSeverity = "";
        for (final RiskCountView vulnerability : vulnerabilities) {
            final RiskCountType severity = vulnerability.getCountType();
            final int severityCount = vulnerability.getCount();
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
        assetWrapper.removeFromBlackDuckAssetPanel(AssetPanelLabel.VULNERABILITIES);
        assetWrapper.removeFromBlackDuckAssetPanel(AssetPanelLabel.VULNERABLE_COMPONENTS);
    }

    public void setAssetPolicyData(final VersionBomPolicyStatusView policyStatusView, final AssetWrapper assetWrapper) {
        final PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(policyStatusView);
        final String policyStatus = policyStatusDescription.getPolicyStatusMessage();
        final String overallStatus = policyStatusView.getOverallStatus().prettyPrint();

        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.POLICY_STATUS, policyStatus);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS, overallStatus);
    }

    public void removePolicyData(final AssetWrapper assetWrapper) {
        assetWrapper.removeFromBlackDuckAssetPanel(AssetPanelLabel.POLICY_STATUS);
        assetWrapper.removeFromBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS);
    }

    public Optional<VersionBomPolicyStatusView> checkAssetPolicy(final BlackDuckServicesFactory blackDuckServicesFactory, final ProjectVersionView projectVersionView) throws IntegrationException {
        logger.info("Checking metadata of {}", projectVersionView.getVersionName());
        final ProjectService projectService = blackDuckServicesFactory.createProjectService();
        return projectService.getPolicyStatusForVersion(projectVersionView);
    }

    public void removeAllMetaData(final AssetWrapper assetWrapper) {
        removePolicyData(assetWrapper);
        removeAssetVulnerabilityData(assetWrapper);
    }

}
