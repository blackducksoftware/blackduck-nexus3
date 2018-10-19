package com.synopsys.integration.blackduck.nexus3.task.metadata;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import com.synopsys.integration.blackduck.nexus3.task.common.CommonDescriptorHelper;

@Named
@Singleton
public class MetaDataTaskDescriptor extends TaskDescriptorSupport {
    public static final String BLACK_DUCK_META_DATA_TASK_ID = "blackduck.asset.property.update";
    public static final String BLACK_DUCK_META_DATA_TASK_NAME = "BlackDuck - Asset Property Update";

    public MetaDataTaskDescriptor() {
        super(BLACK_DUCK_META_DATA_TASK_ID,
            MetaDataTask.class,
            BLACK_DUCK_META_DATA_TASK_NAME,
            VISIBLE,
            EXPOSED,
            CommonDescriptorHelper.getRepositoryField(),
            CommonDescriptorHelper.getPageSizeLimitField()
        );
    }

}
