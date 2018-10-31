package com.synopsys.integration.blackduck.nexus3.mock.model;

import java.util.HashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;

public class MockAsset extends Asset {

    public MockAsset() {
        this("testAsset", new DateTime());
    }

    public MockAsset(final String name, final DateTime updated) {
        final NestedAttributesMap defaultAttributesMap = new NestedAttributesMap(MetadataNodeEntityAdapter.P_ATTRIBUTES, new HashMap<>());
        attributes(defaultAttributesMap);
        name(name);
        blobUpdated(updated);
    }

    @Nullable
    @Override
    public EntityId componentId() {
        return new EntityId() {
            @Nonnull
            @Override
            public String getValue() {
                return "entityId";
            }
        };
    }

    @Nullable
    @Override
    public BlobRef blobRef() {
        return new BlobRef("testNode", "testStore", "testBlob");
    }

}
