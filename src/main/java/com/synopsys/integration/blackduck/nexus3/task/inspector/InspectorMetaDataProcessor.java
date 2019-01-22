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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.core.ProjectRequestBuilder;
import com.synopsys.integration.blackduck.api.generated.component.RiskCountView;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonMetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.common.VulnerabilityLevels;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.ProjectService;
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

    public Optional<ProjectVersionWrapper> getProjectVersionWrapper(final ProjectService projectService, final String name) throws IntegrationException {
        return projectService.getProjectVersion(name, InspectorTask.INSPECTOR_VERSION_NAME);
    }

    public void updateRepositoryMetaData(final ProjectService projectService, final String blackDuckServerUrl, final ProjectVersionView projectVersionView, final Map<String, AssetWrapper> assetWrapperMap,
        final TaskStatus status) throws IntegrationException {
        final List<VersionBomComponentView> versionBomComponentViews = commonMetaDataProcessor.checkAssetVulnerabilities(projectService, projectVersionView);
        for (final VersionBomComponentView versionBomComponentView : versionBomComponentViews) {
            final Set<String> externalIds = versionBomComponentView.getOrigins().stream()
                                                .map(versionBomOriginView -> versionBomOriginView.getExternalId())
                                                .collect(Collectors.toSet());
            logger.debug("Found all externalIds ({}) for component: {}", externalIds, versionBomComponentView.getComponentName());
            for (final String externalId : externalIds) {
                final AssetWrapper assetWrapper = assetWrapperMap.get(externalId);

                if (null == assetWrapper) {
                    logger.warn("{} uploaded to Black Duck, but has not been processed in nexus.", externalId);
                    continue;
                }
                final String blackDuckUrl = projectVersionView.getHref().orElse(blackDuckServerUrl);
                final PolicySummaryStatusType policyStatus = versionBomComponentView.getPolicyStatus();

                logger.info("Found component and updating Asset: {}", assetWrapper.getName());
                if (TaskStatus.FAILURE.equals(status)) {
                    assetWrapper.addFailureToBlackDuckPanel("Was not able to retrieve data from Black Duck.");
                } else {
                    assetWrapper.addSuccessToBlackDuckPanel("Successfully pulled inspection data from Black Duck.");
                    assetWrapper.addToBlackDuckAssetPanel(AssetPanelLabel.BLACKDUCK_URL, blackDuckUrl);
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

    public ProjectVersionView getOrCreateProjectVersion(final ProjectService projectService, final String repoName) throws IntegrationException {
        final Optional<ProjectVersionWrapper> projectVersionWrapperOptional = getProjectVersionWrapper(projectService, repoName);
        if (projectVersionWrapperOptional.isPresent()) {
            final ProjectVersionView projectVersionView = projectVersionWrapperOptional.get().getProjectVersionView();
            return projectVersionView;
        } else {
            logger.debug("Creating project in Black Duck : {}", repoName);
            final ProjectRequestBuilder projectRequestBuilder = new ProjectRequestBuilder();
            projectRequestBuilder.setProjectName(repoName);
            projectRequestBuilder.setVersionName(INSPECTOR_VERSION_NAME);
            final ProjectVersionWrapper projectVersionWrapper = projectService.createProject(projectRequestBuilder.build());
            return projectVersionWrapper.getProjectVersionView();
        }
    }

    private void addVulnerabilityStatus(final AssetWrapper assetWrapper, final VersionBomComponentView versionBomComponentView) {
        final VulnerabilityLevels vulnerabilityLevels = new VulnerabilityLevels();
        final List<RiskCountView> riskCountViews = versionBomComponentView.getSecurityRiskProfile().getCounts();
        logger.info("Counting vulnerabilities");
        commonMetaDataProcessor.addAllAssetVulnerabilityCounts(riskCountViews, vulnerabilityLevels);
        commonMetaDataProcessor.setAssetVulnerabilityData(vulnerabilityLevels, assetWrapper);
    }
}
