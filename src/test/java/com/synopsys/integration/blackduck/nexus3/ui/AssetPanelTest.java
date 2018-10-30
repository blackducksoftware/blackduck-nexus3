package com.synopsys.integration.blackduck.nexus3.ui;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;

public class AssetPanelTest extends TestSupport {

    Asset asset;
    AssetPanelLabel testLabel = AssetPanelLabel.BLACKDUCK_URL;

    @Before
    public void init() {
        asset = new Asset();
        final NestedAttributesMap defaultAttributesMap = new NestedAttributesMap(MetadataNodeEntityAdapter.P_ATTRIBUTES, new HashMap<>());
        asset.attributes(defaultAttributesMap);
    }

    @Test
    public void testAddToBlackDuckPanel() {
        final AssetPanel assetPanel = new AssetPanel(asset);
        assetPanel.addToBlackDuckPanel(testLabel, true);

        final Boolean foundItem = (Boolean) asset.attributes().child(AssetPanel.BLACKDUCK_CATEGORY).get(testLabel.getLabel());
        Assert.assertNotNull(foundItem);
        Assert.assertTrue(foundItem);
    }

    @Test
    public void testGetFromBlackDuckPanel() {
        asset.attributes().child(AssetPanel.BLACKDUCK_CATEGORY).set(testLabel.getLabel(), "true");

        final AssetPanel assetPanel = new AssetPanel(asset);
        final String result = assetPanel.getFromBlackDuckPanel(testLabel);
        final boolean keyIsFound = Boolean.parseBoolean(result);

        Assert.assertTrue(keyIsFound);
    }

    @Test
    public void testRemoveFromBlackDuckPanel() {
        asset.attributes().child(AssetPanel.BLACKDUCK_CATEGORY).set(testLabel.getLabel(), "true");

        final AssetPanel assetPanel = new AssetPanel(asset);

        final String result = assetPanel.getFromBlackDuckPanel(testLabel);
        Assert.assertNotNull(result);

        assetPanel.removeFromBlackDuckPanel(testLabel);

        final String nullResult = assetPanel.getFromBlackDuckPanel(testLabel);
        Assert.assertNull(nullResult);
    }
}
