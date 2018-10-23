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
package com.synopsys.integration.blackduck.nexus3.task.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.TaskStatus;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonMetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonRepositoryTaskHelper;
import com.synopsys.integration.blackduck.nexus3.task.inspector.InspectorMetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.task.scan.ScanMetaDataProcessor;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;

@Named
public class MetaDataTask extends RepositoryTaskSupport {
    private final Logger logger = createLogger();
    private final CommonRepositoryTaskHelper commonRepositoryTaskHelper;
    private final QueryManager queryManager;
    private final CommonMetaDataProcessor commonMetaDataProcessor;
    private final Type proxyType;
    private final InspectorMetaDataProcessor inspectorMetaDataProcessor;
    private final ScanMetaDataProcessor scanMetaDataProcessor;

    @Inject
    public MetaDataTask(final CommonRepositoryTaskHelper commonRepositoryTaskHelper, final QueryManager queryManager, final CommonMetaDataProcessor commonMetaDataProcessor, final InspectorMetaDataProcessor inspectorMetaDataProcessor,
        final ScanMetaDataProcessor scanMetaDataProcessor, @Named(ProxyType.NAME) final Type proxyType) {
        this.commonRepositoryTaskHelper = commonRepositoryTaskHelper;
        this.queryManager = queryManager;
        this.commonMetaDataProcessor = commonMetaDataProcessor;
        this.inspectorMetaDataProcessor = inspectorMetaDataProcessor;
        this.scanMetaDataProcessor = scanMetaDataProcessor;
        this.proxyType = proxyType;
    }

    @Override
    protected void execute(final Repository repository) {
        final Query filteredAssets = createFilteredQuery(Optional.empty());
        PagedResult<Asset> pagedAssets = commonRepositoryTaskHelper.retrievePagedAssets(repository, filteredAssets);
        final Map<String, AssetWrapper> assetWrapperMap = new HashMap<>();
        final boolean isProxyRepo = proxyType.equals(repository.getType());
        while (pagedAssets.hasResults()) {
            logger.debug("Found items in the DB.");
            for (final Asset asset : pagedAssets.getTypeList()) {
                final AssetWrapper assetWrapper = new AssetWrapper(asset, repository, queryManager);
                try {
                    if (!isProxyRepo) {
                        final String blackDuckUrl = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.BLACKDUCK_URL);
                        scanMetaDataProcessor.updateRepositoryMetaData(assetWrapper, blackDuckUrl);
                    } else {
                        final String originId = assetWrapper.getFromBlackDuckAssetPanel(AssetPanelLabel.ASSET_ORIGIN_ID);
                        assetWrapperMap.put(originId, assetWrapper);
                    }
                } catch (final IntegrationException e) {
                    commonMetaDataProcessor.removeAllMetaData(assetWrapper);
                    assetWrapper.updateAsset();
                    throw new TaskInterruptedException("Problem checking metadata: " + e.getMessage(), true);
                }
            }

            final Query nextPage = createFilteredQuery(pagedAssets.getLastName());
            pagedAssets = commonRepositoryTaskHelper.retrievePagedAssets(repository, nextPage);
        }

        if (isProxyRepo) {
            try {
                final String projectName = repository.getName();
                final ProjectVersionWrapper projectVersionWrapper = inspectorMetaDataProcessor.getProjectVersionWrapper(projectName);
                inspectorMetaDataProcessor.updateRepositoryMetaData(projectVersionWrapper.getProjectVersionView(), assetWrapperMap, TaskStatus.SUCCESS);
            } catch (final IntegrationException e) {
                throw new TaskInterruptedException("Problem retrieving project from Hub: " + e.getMessage(), true);
            }
        }
    }

    private Query createFilteredQuery(final Optional<String> lastNameUsed) {
        final Query.Builder pagedQueryBuilder = commonRepositoryTaskHelper.createPagedQuery(lastNameUsed);
        final String blackDuckDbPath = commonRepositoryTaskHelper.getBlackDuckPanelPath(AssetPanelLabel.TASK_STATUS);
        pagedQueryBuilder.and(blackDuckDbPath).eq(TaskStatus.SUCCESS);
        return pagedQueryBuilder.build();
    }

    @Override
    protected boolean appliesTo(final Repository repository) {
        return commonRepositoryTaskHelper.doesRepositoryApply(repository, getRepositoryField());
    }

    @Override
    public String getMessage() {
        return commonRepositoryTaskHelper.getTaskMessage(MetaDataTaskDescriptor.BLACK_DUCK_META_DATA_TASK_NAME, getRepositoryField());
    }

}
