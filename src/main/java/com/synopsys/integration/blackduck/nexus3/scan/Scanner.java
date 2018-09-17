package com.synopsys.integration.blackduck.nexus3.scan;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.exception.HubIntegrationException;
import com.synopsys.integration.blackduck.signaturescanner.ScanJob;
import com.synopsys.integration.blackduck.signaturescanner.ScanJobBuilder;
import com.synopsys.integration.blackduck.signaturescanner.ScanJobManager;
import com.synopsys.integration.blackduck.signaturescanner.ScanJobOutput;
import com.synopsys.integration.blackduck.signaturescanner.command.ScanTarget;
import com.synopsys.integration.exception.EncryptionException;
import com.synopsys.integration.log.Slf4jIntLogger;

public class Scanner {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final HubServerConfig hubServerConfig;
    private final ScanConfig scanConfig;

    public Scanner(final HubServerConfig hubServerConfig, final ScanConfig scanConfig) {
        this.hubServerConfig = hubServerConfig;
        this.scanConfig = scanConfig;
    }

    public ScanJob createScanJob(final Set<String> pathsToScan) throws EncryptionException {
        final ScanJobBuilder scanJobBuilder = new ScanJobBuilder()
                                                  .fromHubServerConfig(hubServerConfig)
                                                  .scanMemoryInMegabytes(scanConfig.getMemoryMB())
                                                  .dryRun(scanConfig.isDryRun())
                                                  .installDirectory(new File(scanConfig.getInstallDirectory()))
                                                  .outputDirectory(new File(scanConfig.getOutputDirectory()))
                                                  .projectAndVersionNames(scanConfig.getProjectName(), scanConfig.getProjectVersion());

        for (final String path : pathsToScan) {
            final ScanTarget scanTarget = ScanTarget.createBasicTarget(path);
            scanJobBuilder.addTarget(scanTarget);
        }

        final ScanJob scanJob = scanJobBuilder.build();
        return scanJob;
    }

    public ScanJobOutput startScanJob(final ScanJob scanJob) throws EncryptionException, IOException, HubIntegrationException {
        final ScanJobManager scanJobManager = ScanJobManager.createDefaultScanManager(new Slf4jIntLogger(logger), hubServerConfig);
        return scanJobManager.executeScans(scanJob);
    }

}
