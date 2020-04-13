package com.synopsys.integration.blackduck.nexus3.task.scan;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.sonatype.nexus.repository.Repository;

import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationService;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.SignatureScannerService;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.ProjectService;

public class ScanConfiguration {
    private final String exceptionMessage;

    private final Repository repository;
    private final boolean alwaysScan;
    private final boolean redoFailures;

    private final BlackDuckServerConfig blackDuckServerConfig;
    private final SignatureScannerService signatureScannerService;
    private final CodeLocationCreationService codeLocationCreationService;
    private final BlackDuckService blackDuckService;
    private final ProjectService projectService;

    private final File workingBlackDuckDirectory;
    private final File tempFileStorage;
    private final File outputDirectory;

    public static ScanConfiguration createConfigurationWithError(String exceptionMessage, Repository repository, boolean alwaysScan, boolean redoFailures) {
        return new ScanConfiguration(exceptionMessage, repository, alwaysScan, redoFailures, null, null, null,
            null, null, null, null, null);
    }

    public static ScanConfiguration createConfiguration(Repository repository, boolean alwaysScan, boolean redoFailures, BlackDuckServerConfig blackDuckServerConfig,
        SignatureScannerService signatureScannerService, CodeLocationCreationService codeLocationCreationService, BlackDuckService blackDuckService, ProjectService projectService,
        File workingBlackDuckDirectory, File tempFileStorage, File outputDirectory) {
        return new ScanConfiguration(null, repository, alwaysScan, redoFailures, blackDuckServerConfig, signatureScannerService, codeLocationCreationService,
            blackDuckService, projectService, workingBlackDuckDirectory, tempFileStorage, outputDirectory);
    }

    private ScanConfiguration(String exceptionMessage, Repository repository, boolean alwaysScan, boolean redoFailures, BlackDuckServerConfig blackDuckServerConfig,
        SignatureScannerService signatureScannerService, CodeLocationCreationService codeLocationCreationService, BlackDuckService blackDuckService, ProjectService projectService,
        File workingBlackDuckDirectory, File tempFileStorage, File outputDirectory) {
        this.exceptionMessage = exceptionMessage;
        this.repository = repository;
        this.alwaysScan = alwaysScan;
        this.redoFailures = redoFailures;
        this.blackDuckServerConfig = blackDuckServerConfig;
        this.signatureScannerService = signatureScannerService;
        this.codeLocationCreationService = codeLocationCreationService;
        this.blackDuckService = blackDuckService;
        this.projectService = projectService;
        this.workingBlackDuckDirectory = workingBlackDuckDirectory;
        this.tempFileStorage = tempFileStorage;
        this.outputDirectory = outputDirectory;
    }

    public boolean hasErrors() {
        return StringUtils.isNotBlank(exceptionMessage) || null == blackDuckServerConfig || null == signatureScannerService || null == codeLocationCreationService || null == blackDuckService || null == projectService;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public Repository getRepository() {
        return repository;
    }

    public boolean isAlwaysScan() {
        return alwaysScan;
    }

    public boolean isRedoFailures() {
        return redoFailures;
    }

    public BlackDuckServerConfig getBlackDuckServerConfig() {
        return blackDuckServerConfig;
    }

    public SignatureScannerService getSignatureScannerService() {
        return signatureScannerService;
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

    public File getWorkingBlackDuckDirectory() {
        return workingBlackDuckDirectory;
    }

    public File getTempFileStorage() {
        return tempFileStorage;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }
}
