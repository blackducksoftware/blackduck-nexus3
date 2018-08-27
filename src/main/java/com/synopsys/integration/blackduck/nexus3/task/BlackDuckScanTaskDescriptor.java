package com.synopsys.integration.blackduck.nexus3.task;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

@Named
@Singleton
public class BlackDuckScanTaskDescriptor extends TaskDescriptorSupport {
    public static final String BLACK_DUCK_SCAN_TASK_ID = "blackduck.scan";
    public static final String BLACK_DUCK_SCAN_TASK_NAME = "BlackDuck - Repository Scan";

    public BlackDuckScanTaskDescriptor() {
        super(BLACK_DUCK_SCAN_TASK_ID,
                BlackDuckScanTask.class,
                BLACK_DUCK_SCAN_TASK_NAME,
                VISIBLE,
                EXPOSED,
                new RepositoryCombobox(
                        RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID,
                        "Repository",
                        "Repository for BlackDuck to Scan",
                        true));
    }

}
