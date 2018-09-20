package com.synopsys.integration.blackduck.nexus3.ui;

public enum AssetPanelLabel {
    SCAN_STATUS("Scan status"),
    SCAN_HUB_URL("Scan upload location");

    private String label;

    private AssetPanelLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
