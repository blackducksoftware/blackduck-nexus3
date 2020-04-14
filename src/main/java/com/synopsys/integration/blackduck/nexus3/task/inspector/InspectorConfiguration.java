package com.synopsys.integration.blackduck.nexus3.task.inspector;

import org.apache.commons.lang3.StringUtils;
import org.sonatype.nexus.repository.Repository;

import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationService;
import com.synopsys.integration.blackduck.codelocation.bdioupload.BdioUploadService;
import com.synopsys.integration.blackduck.nexus3.task.inspector.dependency.DependencyType;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.ComponentService;
import com.synopsys.integration.blackduck.service.ProjectService;

public class InspectorConfiguration {
    private final String exceptionMessage;

    private final Repository repository;
    private final DependencyType dependencyType;

    private final BlackDuckService blackDuckService;
    private final ProjectService projectService;
    private final CodeLocationCreationService codeLocationCreationService;
    private final BdioUploadService bdioUploadService;
    private final ComponentService componentService;

    public static InspectorConfiguration createConfigurationWithError(String exceptionMessage, Repository repository, DependencyType dependencyType) {
        return new InspectorConfiguration(exceptionMessage, repository, dependencyType, null, null, null, null, null);
    }

    public static InspectorConfiguration createConfiguration(Repository repository, DependencyType dependencyType, BlackDuckService blackDuckService, ProjectService projectService,
        CodeLocationCreationService codeLocationCreationService, BdioUploadService bdioUploadService, ComponentService componentService) {
        return new InspectorConfiguration(null, repository, dependencyType, blackDuckService, projectService, codeLocationCreationService, bdioUploadService, componentService);
    }

    public InspectorConfiguration(String exceptionMessage, Repository repository, DependencyType dependencyType, BlackDuckService blackDuckService, ProjectService projectService,
        CodeLocationCreationService codeLocationCreationService, BdioUploadService bdioUploadService, ComponentService componentService) {
        this.exceptionMessage = exceptionMessage;
        this.repository = repository;
        this.dependencyType = dependencyType;
        this.blackDuckService = blackDuckService;
        this.projectService = projectService;
        this.codeLocationCreationService = codeLocationCreationService;
        this.bdioUploadService = bdioUploadService;
        this.componentService = componentService;
    }

    public boolean hasErrors() {
        return StringUtils.isNotBlank(exceptionMessage) || null == blackDuckService || null == projectService || null == codeLocationCreationService || null == bdioUploadService || null == componentService;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public Repository getRepository() {
        return repository;
    }

    public DependencyType getDependencyType() {
        return dependencyType;
    }

    public BlackDuckService getBlackDuckService() {
        return blackDuckService;
    }

    public ProjectService getProjectService() {
        return projectService;
    }

    public CodeLocationCreationService getCodeLocationCreationService() {
        return codeLocationCreationService;
    }

    public BdioUploadService getBdioUploadService() {
        return bdioUploadService;
    }

    public ComponentService getComponentService() {
        return componentService;
    }
}
