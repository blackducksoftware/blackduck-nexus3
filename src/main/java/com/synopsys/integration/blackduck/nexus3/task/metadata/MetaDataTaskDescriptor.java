package com.synopsys.integration.blackduck.nexus3.task.metadata;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

@Named
@Singleton
public class MetaDataTaskDescriptor extends TaskDescriptorSupport {
    public static final String BLACK_DUCK_POLICY_CHECK_TASK_ID = "blackduck.metadata.check";
    public static final String BLACK_DUCK_POLICY_CHECK_TASK_NAME = "BlackDuck - Repository Policy Check";

    private static final String LABEL_REPOSITORY = "Repository";
    private static final String DESCRIPTION_REPO_NAME = "Type in the repository in which to run the task.";
    private static final RepositoryCombobox FIELD_REPOSITORY = new RepositoryCombobox(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, LABEL_REPOSITORY, DESCRIPTION_REPO_NAME, FormField.MANDATORY).excludingAnyOfTypes(ProxyType.NAME);

    public MetaDataTaskDescriptor() {
        super(BLACK_DUCK_POLICY_CHECK_TASK_ID,
            MetaDataTask.class,
            BLACK_DUCK_POLICY_CHECK_TASK_NAME,
            VISIBLE,
            EXPOSED,
            FIELD_REPOSITORY
        );
    }

}
