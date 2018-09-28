package com.synopsys.integration.blackduck.nexus3.ui;

public enum AssetPanelLabel {
    SCAN_STATUS("Scan status"),
    SCAN_HUB_URL("Scan upload URL"),
    SCAN_OVERALL_POLICY_STATUS("Overall Policy Status"),
    SCAN_POLICY_STATUS("Policy Status"),
    SCAN_TIME("Scan Time"),
    SCAN_VULNERABILITY_STATUS("Vulnerability Status");

    private final String label;

    AssetPanelLabel(final String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
