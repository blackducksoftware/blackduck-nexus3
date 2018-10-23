package com.synopsys.integration.blackduck.nexus3.UI;

import java.util.Collections;

import org.junit.Assert;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.Asset;

import com.synopsys.integration.blackduck.nexus3.ui.AssetPanel;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;

public class AssetPanelTest {

    @Mock
    AssetPanelLabel testKey;

    Asset asset;
    NestedAttributesMap nestedAttributesMap;
    String key = "testKey";

    //    @Before
    public void initKey() {
        asset = new Asset();
        final NestedAttributesMap defaultAttributesMap = new NestedAttributesMap("", Collections.emptyMap());
        asset.attributes(defaultAttributesMap);
        nestedAttributesMap = asset.attributes().child(AssetPanel.BLACKDUCK_CATEGORY);
        Mockito.when(testKey.getLabel()).thenReturn(key);
    }

    //    @Test
    public void testAddToBlackDuckPanel() {
        final AssetPanel assetPanel = new AssetPanel(asset);
        assetPanel.addToBlackDuckPanel(testKey, true);

        final Boolean foundKey = (Boolean) nestedAttributesMap.child(AssetPanel.BLACKDUCK_CATEGORY).get(testKey.getLabel());

        Assert.assertTrue(foundKey);
    }

    //    @Test
    public void testGetFromBlackDuckPanel() {
        nestedAttributesMap.set(key, true);

        final AssetPanel assetPanel = new AssetPanel(asset);
        final Boolean keyIsFound = Boolean.valueOf(assetPanel.getFromBlackDuckPanel(testKey));

        Assert.assertTrue(keyIsFound);
    }

    //    @Test
    public void testRemoveFromBlackDuckPanel() {
        nestedAttributesMap.set(key, true);

        final AssetPanel assetPanel = new AssetPanel(asset);
        assetPanel.removeFromBlackDuckPanel(testKey);

        final String result = assetPanel.getFromBlackDuckPanel(testKey);
        Assert.assertNotNull(result);

        final String nullResult = assetPanel.getFromBlackDuckPanel(testKey);
        Assert.assertNull(nullResult);
    }
}
