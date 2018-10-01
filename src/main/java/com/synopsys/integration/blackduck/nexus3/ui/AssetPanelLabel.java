package com.synopsys.integration.blackduck.nexus3.ui;

public enum AssetPanelLabel {
    TASK_STATUS("Status"),
    HUB_URL("Upload URL"),
    TASK_FINISHED_TIME("Completed On"),
    OVERALL_POLICY_STATUS("Overall Policy Status"),
    POLICY_STATUS("Policy Status"),
    INSPECTOR_LOW_VULNERABILITY("Low Vulnerabilities"),
    INSPECTOR_MEDIUM_VULNERABILITY("Medium Vulnerabilities"),
    INSPECTOR_HIGH_VULNERABILITY("High Vulnerabilities");

    private final String label;

    AssetPanelLabel(final String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
