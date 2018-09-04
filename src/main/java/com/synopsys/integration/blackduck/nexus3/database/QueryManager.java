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

import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Supplier;

@Named
@Singleton
public class QueryManager extends StateGuardLifecycleSupport {

    public Iterable<Asset> findAssetsInRepository(Repository repository) {
        try (StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
            storageTx.begin();

            Query.Builder query = Query.builder();

            return storageTx.findAssets(query.build(), Collections.singletonList(repository));
        }
    }

    public void updateAsset(Repository repository, Asset asset) {
        Supplier<StorageTx> supplier = repository.facet(StorageFacet.class).txSupplier();
        UnitOfWork.begin(supplier);
        try (StorageTx storageTx = supplier.get()) {
            saveAsset(storageTx, asset);
        } finally {
            UnitOfWork.end();
        }
    }

    @TransactionalStoreMetadata
    public void saveAsset(StorageTx storageTx, Asset asset) {
        storageTx.begin();
        storageTx.saveAsset(asset);
        storageTx.commit();
    }

    public Iterable<Component> findComponentsInRepository(Repository repository) {
        try (StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
            storageTx.begin();

            Query.Builder query = Query.builder();

            return storageTx.findComponents(query.build(), Collections.singletonList(repository));
        }
    }

    public void updateComponent(Repository repository, Component component) {
        Supplier<StorageTx> supplier = repository.facet(StorageFacet.class).txSupplier();
        UnitOfWork.begin(supplier);
        try (StorageTx storageTx = supplier.get()) {
            saveComponent(storageTx, component);
        } finally {
            UnitOfWork.end();
        }
    }

    @TransactionalStoreMetadata
    public void saveComponent(StorageTx storageTx, Component component) {
        storageTx.begin();
        storageTx.saveComponent(component);
        storageTx.commit();
    }
}
