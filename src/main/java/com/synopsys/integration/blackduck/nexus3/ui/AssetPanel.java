package com.synopsys.integration.blackduck.nexus3.ui;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.Asset;

public class AssetPanel {
    public static final String BLACKDUCK_CATEGORY = "BlackDuck";

    private final NestedAttributesMap blackDuckNestedAttributes;
    private final NestedAttributesMap assetNestedAttributes;

    public AssetPanel(final Asset asset) {
        blackDuckNestedAttributes = getBlackDuckNestedAttributes(asset.attributes());
        assetNestedAttributes = asset.attributes();
    }

    public String getFromBlackDuckPanel(final AssetPanelLabel label) {
        return (String) blackDuckNestedAttributes.get(label.getLabel());
    }

    public String getFromAssetPanel(final String label) {
        return (String) assetNestedAttributes.get(label);
    }

    public void addToBlackDuckPanel(final AssetPanelLabel label, final Object value) {
        blackDuckNestedAttributes.set(label.getLabel(), value);
    }

    public void addToAssetPanel(final String label, final Object value) {
        assetNestedAttributes.set(label, value);
    }

    // This is used to Add items to the BlackDuck tab in the UI
    private NestedAttributesMap getBlackDuckNestedAttributes(final NestedAttributesMap nestedAttributesMap) {
        return nestedAttributesMap.child(BLACKDUCK_CATEGORY);
    }
}
