package com.synopsys.integration.blackduck.nexus3.task.metadata;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.scheduling.Task;

public class MetaDataTaskDescriptorTest {

    @Test
    public void nameTest() {
        final MetaDataTaskDescriptor metaDataTaskDescriptor = new MetaDataTaskDescriptor();
        final String name = metaDataTaskDescriptor.getName();

        Assert.assertEquals(MetaDataTaskDescriptor.BLACK_DUCK_META_DATA_TASK_NAME, name);
    }

    @Test
    public void idTest() {
        final MetaDataTaskDescriptor metaDataTaskDescriptor = new MetaDataTaskDescriptor();
        final String id = metaDataTaskDescriptor.getId();

        Assert.assertEquals(MetaDataTaskDescriptor.BLACK_DUCK_META_DATA_TASK_ID, id);
    }

    @Test
    public void classTest() {
        final MetaDataTaskDescriptor metaDataTaskDescriptor = new MetaDataTaskDescriptor();
        final Class<? extends Task> taskType = metaDataTaskDescriptor.getType();

        Assert.assertEquals(MetaDataTask.class, taskType);
    }

    @Test
    public void fieldsTest() {
        final MetaDataTaskDescriptor metaDataTaskDescriptor = new MetaDataTaskDescriptor();
        final List<FormField> formFields = metaDataTaskDescriptor.getFormFields();

        final boolean containsRepoField = formFields.stream().anyMatch(field -> RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID.equals(field.getId()));

        Assert.assertEquals(1, formFields.size());
        Assert.assertTrue(containsRepoField);
    }
}
