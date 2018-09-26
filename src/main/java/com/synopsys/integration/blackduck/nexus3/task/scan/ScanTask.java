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

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.exception.HubIntegrationException;
import com.synopsys.integration.blackduck.nexus3.capability.HubCapabilityFinder;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.scan.HubScanner;
import com.synopsys.integration.blackduck.nexus3.scan.model.HubScannerConfig;
import com.synopsys.integration.blackduck.nexus3.scan.model.ScanStatus;
import com.synopsys.integration.blackduck.nexus3.task.BlackDuckTask;
import com.synopsys.integration.blackduck.nexus3.task.TaskFilter;
import com.synopsys.integration.blackduck.nexus3.task.scan.model.ScanTaskConfig;
import com.synopsys.integration.blackduck.nexus3.task.scan.model.ScanTaskFields;
import com.synopsys.integration.blackduck.nexus3.task.scan.model.ScanTaskKeys;
import com.synopsys.integration.blackduck.nexus3.ui.ComponentPanel;
import com.synopsys.integration.blackduck.nexus3.ui.ComponentPanelLabel;
import com.synopsys.integration.blackduck.nexus3.util.BlobConverter;
import com.synopsys.integration.blackduck.nexus3.util.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.util.ProjectNameVersionParser;
import com.synopsys.integration.blackduck.signaturescanner.ScanJob;
import com.synopsys.integration.blackduck.signaturescanner.ScanJobOutput;
import com.synopsys.integration.blackduck.signaturescanner.command.ScanCommandOutput;
import com.synopsys.integration.exception.EncryptionException;

@Named
public class ScanTask extends BlackDuckTask {
    private final Logger logger = createLogger();

    private final QueryManager queryManager;
    private final BlobConverter blobConverter;
    private final DateTimeParser dateTimeParser;
    private final TaskFilter taskFilter;
    private final ProjectNameVersionParser projectNameVersionParser;

    @Inject
    public ScanTask(final ProjectNameVersionParser projectNameVersionParser, final QueryManager queryManager, final BlobConverter blobConverter, final DateTimeParser dateTimeParser,
        final HubCapabilityFinder hubCapabilityFinder, final TaskFilter taskFilter) {
        super("Scan", hubCapabilityFinder);
        this.queryManager = queryManager;
        this.blobConverter = blobConverter;
        this.dateTimeParser = dateTimeParser;
        this.taskFilter = taskFilter;
        this.projectNameVersionParser = projectNameVersionParser;
    }

    @Override
    protected void execute(final Repository repository, final HubServerConfig hubServerConfig) {
        final HubScannerConfig hubScanConfig = getScanConfig();
        final ScanTaskConfig scanTaskConfig = getTaskConfig();
        final HubScanner hubScanner = new HubScanner(hubServerConfig, hubScanConfig);
        final Iterable<Asset> foundAssets = queryManager.findAssetsInRepository(repository);
        for (final Asset asset : foundAssets) {
            final Blob blob = queryManager.getBlob(repository, asset.blobRef());
            final String filename = projectNameVersionParser.retrieveBlobName(blob);

            final boolean doesExtensionMatch = taskFilter.doesExtensionMatch(filename, scanTaskConfig.getFilePatterns());
            final boolean doesRepositoryPathMatch = taskFilter.doesRepositoryPathMatch(asset.name(), scanTaskConfig.getRepositoryPathRegex());

            if (!doesExtensionMatch || !doesRepositoryPathMatch) {
                logger.debug("Binary file did not meet requirements for scan: {}", filename);
                continue;
            }

            final File binaryFile;
            try {
                binaryFile = blobConverter.convertBlobToFile(blob, hubScanConfig.getInstallDirectory());
            } catch (final IOException e) {
                throw new TaskInterruptedException("Error saving blob binary to file", true);
            }

            final Component component = queryManager.getComponent(repository, asset.componentId());
            final String name = component.name();
            final String version = component.version();

            final ComponentPanel componentPanel = new ComponentPanel(component);
            final String scanStatus = componentPanel.getFromBlackDuckPanel(ComponentPanelLabel.SCAN_STATUS);

            boolean shouldScan = scanStatus == null;
            if (!shouldScan) {
                shouldScan = !taskFilter.didArtifactFailScan(Enum.valueOf(ScanStatus.class, scanStatus)) || scanTaskConfig.isRescanFailures();
            }
            final boolean artifactTooOld = taskFilter.isArtifactTooOld(scanTaskConfig.getOldArtifactCutoffDate(), component.lastUpdated());
            final boolean alwaysScan = !artifactTooOld && scanTaskConfig.isAlwaysScan();

            if (alwaysScan || (!artifactTooOld && shouldScan)) {
                logger.info("Scanning item: {}", component.name());
                try {
                    final ScanJob scanJob = hubScanner.createScanJob(binaryFile.getAbsolutePath(), name, version);
                    final ScanJobOutput scanJobOutput = hubScanner.startScanJob(scanJob);
                    final List<ScanCommandOutput> scanOutputs = scanJobOutput.getScanCommandOutputs();
                    final ScanCommandOutput scanResult = scanOutputs.get(0);
                    componentPanel.addToBlackDuckPanel(ComponentPanelLabel.SCAN_STATUS, scanResult.getResult().name());
                    queryManager.updateComponent(repository, component);
                } catch (final EncryptionException | IOException | HubIntegrationException e) {
                    logger.error("Error scanning asset: {}. Reason: {}", name, e.getMessage());
                }
            }

        }
    }

    private ScanTaskConfig getTaskConfig() {
        final String filePatterns = getConfiguration().getString(ScanTaskKeys.FILE_PATTERNS.getParameterKey());
        final String artifactPath = getConfiguration().getString(ScanTaskKeys.REPOSITORY_PATH.getParameterKey());
        final boolean rescanFailures = getConfiguration().getBoolean(ScanTaskKeys.RESCAN_FAILURES.getParameterKey(), false);
        final boolean alwaysScan = getConfiguration().getBoolean(ScanTaskKeys.ALWAYS_SCAN.getParameterKey(), false);

        final String artifactCutoff = getConfiguration().getString(ScanTaskKeys.OLD_ARTIFACT_CUTOFF.getParameterKey());
        final DateTime oldArtifactCutoffDate = dateTimeParser.convertFromStringToDate(artifactCutoff);
        return new ScanTaskConfig(filePatterns, artifactPath, oldArtifactCutoffDate, rescanFailures, alwaysScan);
    }

    private HubScannerConfig getScanConfig() {
        final TaskConfiguration taskConfiguration = getConfiguration();
        final int scanMemory = taskConfiguration.getInteger(ScanTaskKeys.SCAN_MEMORY.getParameterKey(), ScanTaskFields.DEFAULT_SCAN_MEMORY);

        final String workingDirectory = taskConfiguration.getString(ScanTaskKeys.WORKING_DIRECTORY.getParameterKey(), ScanTaskFields.DEFAULT_WORKING_DIRECTORY);
        final File workingBlackDuckDirectory = new File(workingDirectory, "blackduck");
        workingBlackDuckDirectory.mkdir();
        final File outputDirectory = new File(workingBlackDuckDirectory, "output");
        outputDirectory.mkdir();

        return new HubScannerConfig(scanMemory, false, workingBlackDuckDirectory, outputDirectory);
    }

}
