package com.synopsys.integration.blackduck.nexus3.ui;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Component;

import com.synopsys.integration.blackduck.nexus3.database.QueryManager;

public class ComponentPanel {
    private static final String BLACKDUCK_CATEGORY = "Black Duck";

    private final Component component;
    private final Repository repository;
    private final NestedAttributesMap blackDuckNestedAttributes;
    private final NestedAttributesMap componentNestedAttributes;

    public ComponentPanel(final Repository repository, final Component component) {
        this.repository = repository;
        this.component = component;
        blackDuckNestedAttributes = getBlackDuckNestedAttributes(component.formatAttributes());
        componentNestedAttributes = component.formatAttributes();
    }

    public void addToBlackDuckPanel(final AssetPanelLabel label, final Object value) {
        blackDuckNestedAttributes.set(label.getLabel(), value);
    }

    public void addToComponentPanel(final String label, final Object value) {
        componentNestedAttributes.set(label, value);
    }

    public void savePanel(final QueryManager queryManager) {
        queryManager.updateComponent(repository, component);
    }

    // This is used to Add items to the BlackDuck tab in the UI
    private NestedAttributesMap getBlackDuckNestedAttributes(final NestedAttributesMap nestedAttributesMap) {
        return nestedAttributesMap.child(BLACKDUCK_CATEGORY);
    }
}
