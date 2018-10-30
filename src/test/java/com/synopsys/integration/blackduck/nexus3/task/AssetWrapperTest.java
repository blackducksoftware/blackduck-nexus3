package com.synopsys.integration.blackduck.nexus3.task;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;

import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanel;

public class AssetWrapperTest extends TestSupport {

    @Mock
    Component component;

    @Mock
    Blob blob;

    @Test
    public void getComponentTest() {
        final Asset asset = new Asset();
        final QueryManager queryManager = Mockito.mock(QueryManager.class);
        Mockito.when(queryManager.getComponent(Mockito.any(), Mockito.any())).thenReturn(component);

        final AssetWrapper assetWrapper = new AssetWrapper(asset, null, queryManager);
        final Component foundComponent = assetWrapper.getComponent();

        Assert.assertNotNull(foundComponent);
    }

    @Test
    public void getBlobTest() {
        final Asset asset = new Asset();
        final QueryManager queryManager = Mockito.mock(QueryManager.class);
        Mockito.when(queryManager.getBlob(Mockito.any(), Mockito.any())).thenReturn(blob);

        final AssetWrapper assetWrapper = new AssetWrapper(asset, null, queryManager);
        final Blob foundBlob = assetWrapper.getBlob();

        Assert.assertNotNull(foundBlob);
    }

    @Test
    public void getAssetPanelTest() {
        final Asset asset = new Asset();
        final NestedAttributesMap defaultAttributesMap = new NestedAttributesMap(MetadataNodeEntityAdapter.P_ATTRIBUTES, new HashMap<>());
        asset.attributes(defaultAttributesMap);
        final AssetWrapper assetWrapper = new AssetWrapper(asset, null, null);
        final AssetPanel assetPanel = assetWrapper.getAssetPanel();

        Assert.assertNotNull(assetPanel);
    }
}
