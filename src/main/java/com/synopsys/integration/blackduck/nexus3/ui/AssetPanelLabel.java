package com.synopsys.integration.blackduck.nexus3.ui;

public enum AssetPanelLabel {
    TASK_STATUS("status"),
    BLACKDUCK_URL("upload_url"),
    TASK_FINISHED_TIME("completed_by"),
    OVERALL_POLICY_STATUS("policy_status_overall"),
    POLICY_STATUS("policy_status"),
    LOW_VULNERABILITY("vulnerabilities_low"),
    MEDIUM_VULNERABILITY("vulnerabilities_medium"),
    HIGH_VULNERABILITY("vulnerabilities_high");

    private final String label;

    AssetPanelLabel(final String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
