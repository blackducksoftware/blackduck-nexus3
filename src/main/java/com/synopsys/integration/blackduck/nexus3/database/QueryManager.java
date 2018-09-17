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
package com.synopsys.integration.blackduck.nexus3.database;

import java.util.Collections;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;

@Named
@Singleton
public class QueryManager {

    public Iterable<Asset> findAssetsInRepository(final Repository repository) {
        try (final StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
            storageTx.begin();

            final Query.Builder query = Query.builder();

            return storageTx.findAssets(query.build(), Collections.singletonList(repository));
        }
    }

    public void updateAsset(final Repository repository, final Asset asset) {
        try (final StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
            storageTx.begin();
            storageTx.saveAsset(asset);
            storageTx.commit();
        }
    }

    public Blob getBlob(final Repository repository, final Asset asset) {
        try (final StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
            return storageTx.getBlob(asset.blobRef());
        }
    }

}
