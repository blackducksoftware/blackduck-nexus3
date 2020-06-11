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

import static com.synopsys.integration.blackduck.nexus3.task.inspector.InspectorTask.INSPECTOR_VERSION_NAME;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.component.ComponentVersionRiskProfileRiskDataCountsView;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyStatusType;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionComponentView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.manual.throwaway.generated.component.VersionBomOriginView;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonMetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.task.common.VulnerabilityLevels;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.exception.IntegrationException;

@Named
@Singleton
public class InspectorMetaDataProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final CommonMetaDataProcessor commonMetaDataProcessor;
    private final DateTimeParser dateTimeParser;

    @Inject
    public InspectorMetaDataProcessor(CommonMetaDataProcessor commonMetaDataProcessor, DateTimeParser dateTimeParser) {
        this.commonMetaDataProcessor = commonMetaDataProcessor;
        this.dateTimeParser = dateTimeParser;
    }

    public void updateRepositoryMetaData(BlackDuckService blackDuckService, String blackDuckServerUrl, ProjectVersionView projectVersionView, Map<String, AssetWrapper> assetWrapperMap,
        TaskStatus status) throws IntegrationException {
        List<ProjectVersionComponentView> versionComponentViews = commonMetaDataProcessor.checkAssetVulnerabilities(blackDuckService, projectVersionView);
        for (ProjectVersionComponentView versionComponentView : versionComponentViews) {
            Set<String> externalIds = versionComponentView.getOrigins().stream()
                                          .map(VersionBomOriginView::getExternalId)
                                          .collect(Collectors.toSet());
            logger.debug("Found all externalIds ({}) for component: {}", externalIds, versionComponentView.getComponentName());
            for (String externalId : externalIds) {
                AssetWrapper assetWrapper = assetWrapperMap.get(externalId);

                if (null == assetWrapper) {
                    logger.warn("{} uploaded to Black Duck, but has not been processed in nexus.", externalId);
                    continue;
                }
                String blackDuckUrl = projectVersionView.getHref().orElse(blackDuckServerUrl);
                PolicyStatusType policyStatus = versionComponentView.getPolicyStatus();

                logger.info("Found component and updating Asset: {}", assetWrapper.getName());
                if (TaskStatus.FAILURE.equals(status)) {
                    assetWrapper.addFailureToBlackDuckPanel("Was not able to retrieve data from Black Duck.");
                } else {
                    assetWrapper.addSuccessToBlackDuckPanel("Successfully pulled inspection data from Black Duck.");
                    assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.BLACKDUCK_URL, blackDuckUrl);
                    assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS, policyStatus.prettyPrint());
                    assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
                    addVulnerabilityStatus(assetWrapper, versionComponentView);
                }
                assetWrapper.updateAsset();
                assetWrapperMap.remove(externalId);
            }
        }

        logger.debug("Currently have following items in asset map: {}", assetWrapperMap);
        for (AssetWrapper assetWrapper : assetWrapperMap.values()) {
            logger.warn("Asset was not found in Black Duck, {}", assetWrapper.getName());
            assetWrapper.addComponentNotFoundToBlackDuckPanel("Black Duck was not able to find this component.");
            assetWrapper.updateAsset();
        }
    }

    public ProjectVersionView getOrCreateProjectVersion(BlackDuckService blackDuckService, ProjectService projectService, String repoName) throws IntegrationException {
        return commonMetaDataProcessor.getOrCreateProjectVersion(blackDuckService, projectService, repoName, INSPECTOR_VERSION_NAME);
    }

    private void addVulnerabilityStatus(AssetWrapper assetWrapper, ProjectVersionComponentView versionComponentView) {
        VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();
        List<ComponentVersionRiskProfileRiskDataCountsView> riskCountViews = versionComponentView.getSecurityRiskProfile().getCounts();
        logger.info("Counting vulnerabilities");
        commonMetaDataProcessor.addAllAssetVulnerabilityCounts(riskCountViews, vulnerabilityLevels);
        commonMetaDataProcessor.setAssetVulnerabilityData(vulnerabilityLevels, assetWrapper);
    }
}
