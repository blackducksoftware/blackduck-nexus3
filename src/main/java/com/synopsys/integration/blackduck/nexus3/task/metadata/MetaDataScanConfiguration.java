package com.synopsys.integration.blackduck.nexus3.task.metadata;

import org.apache.commons.lang3.StringUtils;
import org.sonatype.nexus.repository.Repository;

import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationService;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.ProjectBomService;
import com.synopsys.integration.blackduck.service.ProjectService;

public class MetaDataScanConfiguration {
    private final String exceptionMessage;

    private final Repository repository;
    private final boolean isProxyRepo;
    private final AssetPanelLabel assetStatusLabel;

    private final CodeLocationCreationService codeLocationCreationService;
    private final BlackDuckService blackDuckService;
    private final ProjectService projectService;
    private final ProjectBomService projectBomService;

    public static MetaDataScanConfiguration createConfigurationWithError(String exceptionMessage, Repository repository, boolean isProxyRepo, AssetPanelLabel assetStatusLabel) {
        return new MetaDataScanConfiguration(exceptionMessage, repository, isProxyRepo, assetStatusLabel, null, null, null, null);
    }

    public static MetaDataScanConfiguration createConfiguration(Repository repository, boolean isProxyRepo, AssetPanelLabel assetStatusLabel, CodeLocationCreationService codeLocationCreationService, BlackDuckService blackDuckService,
        ProjectService projectService, ProjectBomService projectBomService) {
        return new MetaDataScanConfiguration(null, repository, isProxyRepo, assetStatusLabel, codeLocationCreationService, blackDuckService, projectService, projectBomService);
    }

    private MetaDataScanConfiguration(String exceptionMessage, Repository repository, boolean isProxyRepo, AssetPanelLabel assetStatusLabel, CodeLocationCreationService codeLocationCreationService, BlackDuckService blackDuckService,
        ProjectService projectService, ProjectBomService projectBomService) {
        this.exceptionMessage = exceptionMessage;
        this.repository = repository;
        this.isProxyRepo = isProxyRepo;
        this.assetStatusLabel = assetStatusLabel;
        this.codeLocationCreationService = codeLocationCreationService;
        this.blackDuckService = blackDuckService;
        this.projectService = projectService;
        this.projectBomService = projectBomService;
    }

    public boolean hasErrors() {
        return StringUtils.isNotBlank(exceptionMessage) || null == codeLocationCreationService || null == blackDuckService || null == projectService;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public Repository getRepository() {
        return repository;
    }

    public boolean isProxyRepo() {
        return isProxyRepo;
    }

    public AssetPanelLabel getAssetStatusLabel() {
        return assetStatusLabel;
    }

    public CodeLocationCreationService getCodeLocationCreationService() {
        return codeLocationCreationService;
    }

    public BlackDuckService getBlackDuckService() {
        return blackDuckService;
    }

    public ProjectService getProjectService() {
        return projectService;
    }

    public ProjectBomService getProjectBomService() {
        return projectBomService;
    }
}
