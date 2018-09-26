package com.synopsys.integration.blackduck.nexus3.ui;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.Component;

public class ComponentPanel {
    private static final String BLACKDUCK_CATEGORY = "Black Duck";

    private final NestedAttributesMap blackDuckNestedAttributes;
    private final NestedAttributesMap componentNestedAttributes;

    public ComponentPanel(final Component component) {
        blackDuckNestedAttributes = getBlackDuckNestedAttributes(component.formatAttributes());
        componentNestedAttributes = component.formatAttributes();
    }

    public String getFromBlackDuckPanel(final ComponentPanelLabel label) {
        return (String) blackDuckNestedAttributes.get(label.getLabel());
    }

    public String getFromComponentPanel(final String label) {
        return (String) componentNestedAttributes.get(label);
    }

    public void addToBlackDuckPanel(final ComponentPanelLabel label, final Object value) {
        blackDuckNestedAttributes.set(label.getLabel(), value);
    }

    public void addToComponentPanel(final String label, final Object value) {
        componentNestedAttributes.set(label, value);
    }

    // This is used to Add items to the BlackDuck tab in the UI
    private NestedAttributesMap getBlackDuckNestedAttributes(final NestedAttributesMap nestedAttributesMap) {
        return nestedAttributesMap.child(BLACKDUCK_CATEGORY);
    }
}
