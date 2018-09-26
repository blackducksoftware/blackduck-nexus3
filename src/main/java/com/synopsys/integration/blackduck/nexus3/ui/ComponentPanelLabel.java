package com.synopsys.integration.blackduck.nexus3.ui;

public enum ComponentPanelLabel {
    SCAN_STATUS("Scan status"),
    SCAN_HUB_URL("Scan upload URL"),
    SCAN_OVERALL_POLICY_STATUS("Overall Policy Status"),
    SCAN_POLICY_STATUS("Policy Status"),
    SCAN_TIME("Scan Time"),
    SCAN_VLNERABILITY_STATSU("Vulnerability Status");

    private final String label;

    private ComponentPanelLabel(final String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
