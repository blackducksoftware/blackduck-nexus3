package com.synopsys.integration.blackduck.nexus3.ui;

public enum AssetPanelLabel {
    TASK_STATUS("status"),
    HUB_URL("upload_url"),
    TASK_FINISHED_TIME("completed_by"),
    OVERALL_POLICY_STATUS("overall_policy_status"),
    POLICY_STATUS("policy_status"),
    INSPECTOR_LOW_VULNERABILITY("low_vulnerabilities"),
    INSPECTOR_MEDIUM_VULNERABILITY("medium_vulnerabilities"),
    INSPECTOR_HIGH_VULNERABILITY("high_vulnerabilities");

    private final String label;

    AssetPanelLabel(final String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
