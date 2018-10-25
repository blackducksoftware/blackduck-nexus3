package com.synopsys.integration.blackduck.nexus3.ui;

//@RunWith(MockitoJUnitRunner.class)
public class AssetPanelTest {

    //    @Mock
    //    AssetPanelLabel testKey;
    //
    //    Asset asset;
    //    NestedAttributesMap nestedAttributesMap;
    //    String key = "testKey";
    //
    //    @Before
    //    public void initKey() {
    //        asset = new Asset();
    //        final NestedAttributesMap defaultAttributesMap = new NestedAttributesMap(MetadataNodeEntityAdapter.P_ATTRIBUTES, new HashMap<>());
    //        asset.attributes(defaultAttributesMap);
    //        nestedAttributesMap = asset.attributes().child(AssetPanel.BLACKDUCK_CATEGORY);
    //        Mockito.when(testKey.getLabel()).thenReturn(key);
    //    }
    //
    //    @Test
    //    public void testAddToBlackDuckPanel() {
    //        final AssetPanel assetPanel = new AssetPanel(asset);
    //        assetPanel.addToBlackDuckPanel(testKey, true);
    //
    //        final Boolean foundKey = (Boolean) nestedAttributesMap.child(AssetPanel.BLACKDUCK_CATEGORY).get(testKey.getLabel());
    //
    //        Assert.assertTrue(foundKey);
    //    }
    //
    //    @Test
    //    public void testGetFromBlackDuckPanel() {
    //        nestedAttributesMap.set(key, true);
    //
    //        final AssetPanel assetPanel = new AssetPanel(asset);
    //        final Boolean keyIsFound = Boolean.valueOf(assetPanel.getFromBlackDuckPanel(testKey));
    //
    //        Assert.assertTrue(keyIsFound);
    //    }
    //
    //    @Test
    //    public void testRemoveFromBlackDuckPanel() {
    //        nestedAttributesMap.set(key, true);
    //
    //        final AssetPanel assetPanel = new AssetPanel(asset);
    //        assetPanel.removeFromBlackDuckPanel(testKey);
    //
    //        final String result = assetPanel.getFromBlackDuckPanel(testKey);
    //        Assert.assertNotNull(result);
    //
    //        final String nullResult = assetPanel.getFromBlackDuckPanel(testKey);
    //        Assert.assertNull(nullResult);
    //    }
}
