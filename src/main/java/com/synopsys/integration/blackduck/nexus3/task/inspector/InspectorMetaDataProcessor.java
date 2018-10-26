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
package com.synopsys.integration.blackduck.nexus3.task.inspector;

import java.util.List;
import java.util.Map;
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
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
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
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final CommonMetaDataProcessor commonMetaDataProcessor;
    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final DateTimeParser dateTimeParser;

    @Inject
    public InspectorMetaDataProcessor(final CommonMetaDataProcessor commonMetaDataProcessor, final CommonRepositoryTaskHelper commonRepositoryTaskHelper, final DateTimeParser dateTimeParser) {
        this.commonMetaDataProcessor = commonMetaDataProcessor;
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.dateTimeParser = dateTimeParser;
    }

    public ProjectVersionWrapper getProjectVersionWrapper(final String name) throws IntegrationException {
        return commonRepositoryTaskHelper.getProjectVersionWrapper(name, InspectorTask.INSPECTOR_CODE_LOCATION_NAME);
    }

    public void updateRepositoryMetaData(final ProjectVersionView projectVersionView, final Map<String, AssetWrapper> assetWrapperMap, final TaskStatus status) throws IntegrationException {
        logger.debug("Currently have following items in asset map: {}", assetWrapperMap);
        final List<VersionBomComponentView> versionBomComponentViews = commonMetaDataProcessor.checkAssetVulnerabilities(projectVersionView);
        for (final VersionBomComponentView versionBomComponentView : versionBomComponentViews) {
            final Set<String> externalIds = versionBomComponentView.origins.stream()
                                                .map(versionBomOriginView -> versionBomOriginView.externalId)
                                                .collect(Collectors.toSet());
            logger.debug("Found all externalIds ({}) for component: {}", externalIds, versionBomComponentView.componentName);
            for (final String externalId : externalIds) {
                final AssetWrapper assetWrapper = assetWrapperMap.get(externalId);

                final String componentUrl = versionBomComponentView.componentVersion;
                final PolicySummaryStatusType policyStatus = versionBomComponentView.policyStatus;

                logger.info("Found component and updating Asset: {}", assetWrapper.getName());
                if (TaskStatus.FAILURE.equals(status)) {
                    assetWrapper.addFailureToBlackDuckPanel("Was not able to retrieve data from Black Duck.");
                } else {
                    assetWrapper.addSuccessToBlackDuckPanel("Successfully pulled inspection data from Black Duck.");
                    assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.BLACKDUCK_URL, componentUrl);
                    assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS, policyStatus.prettyPrint());
                    assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
                    addVulnerabilityStatus(assetWrapper, versionBomComponentView);
                }
                assetWrapper.updateAsset();
                assetWrapperMap.remove(externalId);
            }
        }

        logger.debug("Currently have following items in asset map: {}", assetWrapperMap);
        for (final AssetWrapper assetWrapper : assetWrapperMap.values()) {
            logger.warn("Asset was not found in Black Duck, {}", assetWrapper.getName());
            assetWrapper.addComponentNotFoundToBlackDuckPanel("Black Duck was not able to find this component.");
            assetWrapper.updateAsset();
        }
    }

    private void addVulnerabilityStatus(final AssetWrapper assetWrapper, final VersionBomComponentView versionBomComponentView) {
        final VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();
        final List<RiskCountView> riskCountViews = versionBomComponentView.securityRiskProfile.counts;
        logger.info("Counting vulnerabilities");
        commonMetaDataProcessor.addAllAssetVulnerabilityCounts(riskCountViews, vulnerabilityLevels);
        commonMetaDataProcessor.setAssetVulnerabilityData(vulnerabilityLevels, assetWrapper);
    }
}
