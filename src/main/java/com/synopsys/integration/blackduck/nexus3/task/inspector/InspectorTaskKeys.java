package com.synopsys.integration.blackduck.nexus3.task.inspector;

public enum InspectorTaskKeys {
    FILE_PATTERNS("blackduck.inspector.file.pattern.match.wildcards"),
    OLD_ARTIFACT_CUTOFF("blackduck.inspector.artifact.cutoff"),
    INSPECT_FAILURES("blackduck.inspector.reinspect.failures"),
    ALWAYS_INSPECT("blackduck.inspector.reinspect.always"),
    REPOSITORY_PATH("blackduck.inspector.nexus.artifact.path"),
    PAGING_SIZE("blackduck.inspector.paging.size");

    private final String parameterKey;

    InspectorTaskKeys(final String parameterKey) {
        this.parameterKey = parameterKey;
    }

    public String getParameterKey() {
        return parameterKey;
    }
}
