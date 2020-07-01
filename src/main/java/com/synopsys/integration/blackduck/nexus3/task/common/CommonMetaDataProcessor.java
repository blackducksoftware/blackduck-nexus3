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

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.component.ComponentVersionRiskProfileRiskDataCountsView;
import com.synopsys.integration.blackduck.api.generated.enumeration.ComponentVersionRiskProfileRiskDataCountsCountTypeType;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionComponentView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionPolicyStatusView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectView;
import com.synopsys.integration.blackduck.api.generated.view.TagView;
import com.synopsys.integration.blackduck.nexus3.TagService;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.PolicyStatusDescription;
import com.synopsys.integration.blackduck.service.model.ProjectSyncModel;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.Slf4jIntLogger;

@Named
@Singleton
public class CommonMetaDataProcessor {
    public static final String NEXUS_PROJECT_TAG = "blackduck_nexus3";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void setAssetVulnerabilityData(VulnerabilityLevels vulnerabilityLevels, AssetWrapper assetWrapper) {
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.VULNERABILITIES, vulnerabilityLevels.getAllCounts());
    }

    public List<ProjectVersionComponentView> getBomComponents(BlackDuckService blackDuckService, ProjectVersionView projectVersionView) throws IntegrationException {
        if (!projectVersionView.hasLink(ProjectVersionView.COMPONENTS_LINK)) {
            logger.error(String.format("The '%s' link is missing from the Project Version: '%s'.", ProjectVersionView.COMPONENTS_LINK, projectVersionView.getHref().orElse("MISSING HREF")));
        }
        return blackDuckService.getAllResponses(projectVersionView, ProjectVersionView.COMPONENTS_LINK_RESPONSE);
    }

    public void addAllAssetVulnerabilityCounts(List<ComponentVersionRiskProfileRiskDataCountsView> vulnerabilities, VulnerabilityLevels vulnerabilityLevels) {
        for (ComponentVersionRiskProfileRiskDataCountsView riskCountView : vulnerabilities) {
            ComponentVersionRiskProfileRiskDataCountsCountTypeType riskCountType = riskCountView.getCountType();
            BigDecimal riskCount = riskCountView.getCount();
            vulnerabilityLevels.addXVulnerabilities(riskCountType, riskCount);
        }
    }

    public void addMaxAssetVulnerabilityCounts(List<ComponentVersionRiskProfileRiskDataCountsView> vulnerabilities, VulnerabilityLevels vulnerabilityLevels) {
        Optional<ComponentVersionRiskProfileRiskDataCountsCountTypeType> highestSeverity = vulnerabilities.stream()
                                                                                               .filter(Objects::nonNull)
                                                                                               .filter(this::hasVulnerabilities)
                                                                                               .map(ComponentVersionRiskProfileRiskDataCountsView::getCountType)
                                                                                               .max(Comparator.comparingInt(this::getCountTypePriority));

        highestSeverity.ifPresent(vulnerabilityLevels::addVulnerability);
    }

    private int getCountTypePriority(ComponentVersionRiskProfileRiskDataCountsCountTypeType countType) {
        if (null == countType) {
            return 0;
        }
        switch (countType) {
            case CRITICAL:
                return 10;
            case HIGH:
                return 8;
            case MEDIUM:
                return 6;
            case LOW:
                return 4;
            default:
                return 0;
        }
    }

    private boolean hasVulnerabilities(ComponentVersionRiskProfileRiskDataCountsView riskCountView) {
        int vulnerabilityCount = Optional.ofNullable(riskCountView.getCount())
                                     .map(BigDecimal::intValue)
                                     .orElse(0);
        return vulnerabilityCount > 0;
    }

    public void removeAssetVulnerabilityData(AssetWrapper assetWrapper) {
        assetWrapper.removeFromBlackDuckAssetPanel(AssetPanelLabel.VULNERABILITIES);
        assetWrapper.removeFromBlackDuckAssetPanel(AssetPanelLabel.VULNERABLE_COMPONENTS);
    }

    public void setAssetPolicyData(ProjectVersionPolicyStatusView policyStatusView, AssetWrapper assetWrapper) {
        PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(policyStatusView);
        String policyStatus = policyStatusDescription.getPolicyStatusMessage();
        String overallStatus = policyStatusView.getOverallStatus().prettyPrint();

        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.POLICY_STATUS, policyStatus);
        assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS, overallStatus);
    }

    public void removePolicyData(AssetWrapper assetWrapper) {
        assetWrapper.removeFromBlackDuckAssetPanel(AssetPanelLabel.POLICY_STATUS);
        assetWrapper.removeFromBlackDuckAssetPanel(AssetPanelLabel.OVERALL_POLICY_STATUS);
    }

    public Optional<ProjectVersionPolicyStatusView> checkAssetPolicy(BlackDuckService blackDuckService, ProjectVersionView projectVersionView) throws IntegrationException {
        logger.info("Checking metadata of {}", projectVersionView.getVersionName());
        return blackDuckService.getResponse(projectVersionView, ProjectVersionView.POLICY_STATUS_LINK_RESPONSE);
    }

    public void removeAllMetaData(AssetWrapper assetWrapper) {
        removePolicyData(assetWrapper);
        removeAssetVulnerabilityData(assetWrapper);
    }

    public ProjectVersionView getOrCreateProjectVersion(BlackDuckService blackDuckService, ProjectService projectService, String name, String versionName) throws IntegrationException {
        ProjectVersionWrapper projectVersionWrapper = handleGetOrProjectVersion(projectService, name, versionName);

        TagService tagService = new TagService(blackDuckService, new Slf4jIntLogger(logger));
        ProjectView projectView = projectVersionWrapper.getProjectView();
        Optional<TagView> matchingTag = tagService.findMatchingTag(projectView, NEXUS_PROJECT_TAG);
        if (!matchingTag.isPresent()) {
            logger.debug("Adding tag {} to project {} in Black Duck.", NEXUS_PROJECT_TAG, name);
            TagView tagView = new TagView();
            tagView.setName(NEXUS_PROJECT_TAG);
            tagService.createTag(projectView, tagView);
        }

        return projectVersionWrapper.getProjectVersionView();
    }

    private ProjectVersionWrapper handleGetOrProjectVersion(ProjectService projectService, String name, String versionName) throws IntegrationException {
        logger.debug("Getting project in Black Duck : {}. Version: {}", name, versionName);

        ProjectVersionWrapper projectVersionWrapper = null;
        ProjectSyncModel projectSyncModel = ProjectSyncModel.createWithDefaults(name, versionName);
        Optional<ProjectView> projectViewOptional = projectService.getProjectByName(name);
        if (projectViewOptional.isPresent()) {
            ProjectView projectView = projectViewOptional.get();
            ProjectVersionView projectVersionView = null;
            Optional<ProjectVersionView> projectVersionViewOptional = projectService.getProjectVersion(projectView, versionName);
            if (projectVersionViewOptional.isPresent()) {
                projectVersionView = projectVersionViewOptional.get();
            } else {
                logger.debug("Creating version: {}. In Project {}", versionName, name);
                projectVersionView = projectService.createProjectVersion(projectView, projectSyncModel.createProjectVersionRequest());
            }
            projectVersionWrapper = new ProjectVersionWrapper(projectView, projectVersionView);
        } else {
            logger.debug("Creating project in Black Duck : {}. Version: {}", name, versionName);
            projectVersionWrapper = projectService.createProject(projectSyncModel.createProjectRequest());
        }
        return projectVersionWrapper;
    }
}
