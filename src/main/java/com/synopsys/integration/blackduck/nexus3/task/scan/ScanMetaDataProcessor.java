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
package com.synopsys.integration.blackduck.nexus3.task.scan;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.component.RiskCountView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomPolicyStatusView;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonMetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.task.common.VulnerabilityLevels;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.exception.IntegrationException;

@Named
@Singleton
public class ScanMetaDataProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final CommonMetaDataProcessor commonMetaDataProcessor;
    private final DateTimeParser dateTimeParser;

    @Inject
    public ScanMetaDataProcessor(final CommonMetaDataProcessor commonMetaDataProcessor, final DateTimeParser dateTimeParser) {
        this.commonMetaDataProcessor = commonMetaDataProcessor;
        this.dateTimeParser = dateTimeParser;
    }

    public void updateRepositoryMetaData(final BlackDuckService blackDuckService, final AssetWrapper assetWrapper, final String blackDuckUrl, final ProjectVersionView projectVersionView)
        throws IntegrationException {
        logger.info("Checking vulnerabilities.");
        final List<VersionBomComponentView> versionBomComponentViews = commonMetaDataProcessor.checkAssetVulnerabilities(blackDuckService, projectVersionView);
        final VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();
        for (final VersionBomComponentView versionBomComponentView : versionBomComponentViews) {
            final List<RiskCountView> vulnerabilities = versionBomComponentView.getSecurityRiskProfile().getCounts();
            commonMetaDataProcessor.addMaxAssetVulnerabilityCounts(vulnerabilities, vulnerabilityLevels);
        }
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.VULNERABLE_COMPONENTS, vulnerabilityLevels.getAllCounts());
        logger.info("Checking policies.");
        final Optional<VersionBomPolicyStatusView> policyStatusView = commonMetaDataProcessor.checkAssetPolicy(blackDuckService, projectVersionView);
        if (policyStatusView.isPresent()) {
            commonMetaDataProcessor.setAssetPolicyData(policyStatusView.get(), assetWrapper);
            assetWrapper.addSuccessToBlackDuckPanel("Scan results successfully retrieved from Black Duck.");
        } else {
            assetWrapper.addFailureToBlackDuckPanel("Could not get the policy information for this asset.");
        }
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.BLACKDUCK_URL, blackDuckUrl);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
        assetWrapper.updateAsset();
    }

    public ProjectVersionView getOrCreateProjectVersion(final BlackDuckService blackDuckService, final ProjectService projectService, final String repoName, final String version) throws IntegrationException {
        return commonMetaDataProcessor.getOrCreateProjectVersion(blackDuckService, projectService, repoName, version);
    }

    public String createCodeLocationName(final String repoName, final String name, final String version) {
        return String.join("/", ScanTask.SCAN_CODE_LOCATION_NAME, repoName, name, version);
    }
}
