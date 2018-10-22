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
package com.synopsys.integration.blackduck.nexus3.task;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.joda.time.DateTime;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanel;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;

public class AssetWrapper {
    private final Asset asset;
    private final Repository repository;
    private final QueryManager queryManager;

    private Component associatedComponent;
    private Blob associatedBlob;
    private AssetPanel associatedAssetPanel;

    public AssetWrapper(final Asset asset, final Repository repository, final QueryManager queryManager) {
        this.asset = asset;
        this.repository = repository;
        this.queryManager = queryManager;
    }

    public Component getComponent() {
        if (associatedComponent == null) {
            associatedComponent = queryManager.getComponent(repository, asset.componentId());
        }
        return associatedComponent;
    }

    public Blob getBlob() {
        if (associatedBlob == null) {
            associatedBlob = queryManager.getBlob(repository, asset.blobRef());
        }
        return associatedBlob;
    }

    public AssetPanel getAssetPanel() {
        if (associatedAssetPanel == null) {
            associatedAssetPanel = new AssetPanel(asset);
        }
        return associatedAssetPanel;
    }

    public File getBinaryBlobFile(final File parentDirectory) throws IOException {
        final InputStream blobInputStream = getBlob().getInputStream();
        final File blobFile = new File(parentDirectory, getFilename());
        FileUtils.copyInputStreamToFile(blobInputStream, blobFile);

        return blobFile;
    }

    public void updateAsset() {
        queryManager.updateAsset(repository, asset);
    }

    public String getName() {
        return getComponent().name();
    }

    public String getVersion() {
        return getComponent().version();
    }

    public String getFilename() {
        return getBlob().getHeaders().get(BlobStore.BLOB_NAME_HEADER);
    }

    public String getExtension() {
        return FilenameUtils.getExtension(getFilename());
    }

    public DateTime getComponentLastUpdated() {
        return getComponent().lastUpdated();
    }

    public void addToBlackDuckAssetPanel(final AssetPanelLabel label, final String value) {
        getAssetPanel().addToBlackDuckPanel(label, value);
    }

    public String getFromBlackDuckAssetPanel(final AssetPanelLabel label) {
        return getAssetPanel().getFromBlackDuckPanel(label);
    }

    public void removeFromBlackDuckAssetPanel(final AssetPanelLabel label) {
        getAssetPanel().removeFromBlackDuckPanel(label);
    }

    public Asset getAsset() {
        return asset;
    }
}
