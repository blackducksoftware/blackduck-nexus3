package com.synopsys.integration.blackduck.nexus3.task.inspector;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import com.synopsys.integration.blackduck.nexus3.task.common.CommonDescriptorHelper;

@Named
@Singleton
public class InspectorTaskDescriptor extends TaskDescriptorSupport {
    public static final String BLACK_DUCK_INSPECTOR_TASK_ID = "blackduck.inspector";
    public static final String BLACK_DUCK_INSPECTOR_TASK_NAME = "BlackDuck - Repository Inspector";

    public InspectorTaskDescriptor() {
        super(BLACK_DUCK_INSPECTOR_TASK_ID,
            InspectorTask.class,
            BLACK_DUCK_INSPECTOR_TASK_NAME,
            VISIBLE,
            EXPOSED,
            getFields()
        );
    }

    public static FormField[] getFields() {
        final FormField[] fields = {
            CommonDescriptorHelper.getRepositoryField().includingAnyOfTypes(ProxyType.NAME),
            CommonDescriptorHelper.getRepositoryPathField(),
            CommonDescriptorHelper.getFilePatternField(),
            CommonDescriptorHelper.getWorkingDirectoryField(),
            CommonDescriptorHelper.getArtifactCutoffDateField(),
            CommonDescriptorHelper.getPageSizeLimitField()
        };
        return fields;
    }

}
