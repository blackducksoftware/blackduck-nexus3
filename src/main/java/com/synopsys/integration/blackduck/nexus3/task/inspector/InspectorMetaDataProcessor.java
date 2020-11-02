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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.synopsys.integration.blackduck.nexus3.task.common.CommonMetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.task.common.VulnerabilityLevels;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.ProjectBomService;
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

    public void updateRepositoryMetaData(ProjectBomService projectBomService, String blackDuckServerUrl, ProjectVersionView projectVersionView, Map<String, AssetWrapper> assetWrapperMap) throws IntegrationException {
        String projectVersionHref = projectVersionView.getHref().orElse("MISSING HREF");
        logger.debug("Checking for components in Project Version: '{}'.", projectVersionHref);
        List<ProjectVersionComponentView> versionComponentViews = commonMetaDataProcessor.getBomComponents(projectBomService, projectVersionView);
        Map<String, AssetWrapper> remainingAssets = new HashMap<>();
        if (versionComponentViews.isEmpty()) {
            logger.error("Could not find components in Project Version: '{}'. Check to see if the Code Locations and scans have finished.", projectVersionHref);
        } else {
            logger.debug("Found '{}' components in Project Version: '{}'.", versionComponentViews.size(), projectVersionHref);
            remainingAssets = processAssetMapAndBlackDuckComponents(versionComponentViews, blackDuckServerUrl, projectVersionView, assetWrapperMap);
        }
        logger.debug("The following assets did not have a matching component: {}", remainingAssets);
        for (AssetWrapper assetWrapper : remainingAssets.values()) {
            logger.warn("This asset was not found in Black Duck, {}", assetWrapper.getName());
            assetWrapper.addComponentNotFoundToBlackDuckPanel("Black Duck was not able to find this component.");
            assetWrapper.updateAsset();
        }
    }

    public ProjectVersionView getOrCreateProjectVersion(BlackDuckService blackDuckService, ProjectService projectService, String repoName) throws IntegrationException {
        return commonMetaDataProcessor.getOrCreateProjectVersion(blackDuckService, projectService, repoName, INSPECTOR_VERSION_NAME);
    }

    public void updateComponentNotFoundStatus(AssetWrapper assetWrapper, String componentMissingMessage) {
        assetWrapper.removeAllBlackDuckData();
        assetWrapper.addComponentNotFoundToBlackDuckPanel(componentMissingMessage);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
        assetWrapper.updateAsset();
    }

    private Map<String, AssetWrapper> processAssetMapAndBlackDuckComponents(List<ProjectVersionComponentView> versionComponentViews, String blackDuckServerUrl,
        ProjectVersionView projectVersionView, Map<String, AssetWrapper> assetWrapperMap) {
        Map<String, ProjectVersionComponentView> externalIdToComponent = new HashMap<>();
        for (ProjectVersionComponentView versionComponentView : versionComponentViews) {
            versionComponentView.getOrigins().stream()
                .map(VersionBomOriginView::getExternalId)
                .forEach(externalId -> externalIdToComponent.put(externalId, versionComponentView));
        }
        Map<String, AssetWrapper> remainingAssets = new HashMap<>(assetWrapperMap);
        for (Map.Entry<String, AssetWrapper> assetWrapperEntry : assetWrapperMap.entrySet()) {
            String externalId = assetWrapperEntry.getKey();
            AssetWrapper assetWrapper = assetWrapperEntry.getValue();

            ProjectVersionComponentView versionComponentView = externalIdToComponent.get(externalId);
            if (null == versionComponentView) {
                String componentNotFoundMessage = String.format("The component %s could not be found in Black Duck.", externalId);
                logger.warn(componentNotFoundMessage);
                updateComponentNotFoundStatus(assetWrapper, componentNotFoundMessage);
                continue;
            }

            processAssetComponent(versionComponentView, blackDuckServerUrl, projectVersionView, assetWrapper);
            remainingAssets.remove(externalId);
        }
        return remainingAssets;
    }

    private void processAssetComponent(ProjectVersionComponentView versionComponentView, String blackDuckServerUrl, ProjectVersionView projectVersionView, AssetWrapper assetWrapper) {
        String blackDuckUrl = projectVersionView.getHref().orElse(blackDuckServerUrl);
        PolicyStatusType policyStatus = versionComponentView.getPolicyStatus();

        logger.debug("Found component and updating Asset: {}:{}", assetWrapper.getName(), assetWrapper.getVersion());
        assetWrapper.addSuccessToBlackDuckPanel("Successfully pulled inspection data from Black Duck.");
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.BLACKDUCK_URL, blackDuckUrl);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS, policyStatus.prettyPrint());
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.TASK_FINISHED_TIME, dateTimeParser.getCurrentDateTime());
        addVulnerabilityStatus(assetWrapper, versionComponentView);
        assetWrapper.updateAsset();
    }

    private void addVulnerabilityStatus(AssetWrapper assetWrapper, ProjectVersionComponentView versionComponentView) {
        VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();
        List<ComponentVersionRiskProfileRiskDataCountsView> riskCountViews = versionComponentView.getSecurityRiskProfile().getCounts();
        logger.trace("Counting vulnerabilities");
        commonMetaDataProcessor.addAllAssetVulnerabilityCounts(riskCountViews, vulnerabilityLevels);
        commonMetaDataProcessor.setAssetVulnerabilityData(vulnerabilityLevels, assetWrapper);
    }

}
