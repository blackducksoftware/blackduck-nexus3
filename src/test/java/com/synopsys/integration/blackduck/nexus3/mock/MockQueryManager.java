package com.synopsys.integration.blackduck.nexus3.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;

import com.synopsys.integration.blackduck.nexus3.database.QueryManager;

public class MockQueryManager extends QueryManager {
    List<Asset> dbAssets;
    Map<EntityId, Component> dbComponents;
    Map<BlobRef, Blob> dbBlobs;

    public MockQueryManager() {
        this(new ArrayList<>(), new HashMap<>(), new HashMap<>());
    }

    public MockQueryManager(final List<Asset> dbAssets, final Map<EntityId, Component> dbComponents, final Map<BlobRef, Blob> dbBlobs) {
        this.dbAssets = dbAssets;
        this.dbComponents = dbComponents;
        this.dbBlobs = dbBlobs;
    }

    @Override
    public Iterable<Asset> findAssetsInRepository(final Repository repository, final Query query) {
        return findAllAssetsInRepository(repository);
    }

    @Override
    public Iterable<Asset> findAllAssetsInRepository(final Repository repository) {
        return dbAssets;
    }

    @Override
    public void updateAsset(final Repository repository, final Asset asset) {
    }

    @Override
    public Blob getBlob(final Repository repository, final BlobRef blobRef) {
        return dbBlobs.get(blobRef);
    }

    @Override
    public Component getComponent(final Repository repository, final EntityId id) {
        return dbComponents.get(id);
    }

    public void addAsset(final Asset asset) {
        dbAssets.add(asset);
    }

    public void addComponent(final EntityId entityId, final Component component) {
        dbComponents.put(entityId, component);
    }

    public void addBlob(final BlobRef blobRef, final Blob blob) {
        dbBlobs.put(blobRef, blob);
    }
}
