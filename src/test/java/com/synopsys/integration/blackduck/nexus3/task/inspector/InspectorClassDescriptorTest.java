package com.synopsys.integration.blackduck.nexus3.task.inspector;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.scheduling.Task;

public class InspectorClassDescriptorTest {

    @Test
    public void nameTest() {
        final InspectorTaskDescriptor inspectorTaskDescriptor = new InspectorTaskDescriptor();
        final String name = inspectorTaskDescriptor.getName();

        Assert.assertEquals(InspectorTaskDescriptor.BLACK_DUCK_INSPECTOR_TASK_NAME, name);
    }

    @Test
    public void idTest() {
        final InspectorTaskDescriptor inspectorTaskDescriptor = new InspectorTaskDescriptor();
        final String id = inspectorTaskDescriptor.getId();

        Assert.assertEquals(InspectorTaskDescriptor.BLACK_DUCK_INSPECTOR_TASK_ID, id);
    }

    @Test
    public void classTest() {
        final InspectorTaskDescriptor inspectorTaskDescriptor = new InspectorTaskDescriptor();
        final Class<? extends Task> taskType = inspectorTaskDescriptor.getType();

        Assert.assertEquals(InspectorTask.class, taskType);
    }

    @Test
    public void fieldsTest() {
        final InspectorTaskDescriptor inspectorTaskDescriptor = new InspectorTaskDescriptor();
        final List<FormField> formFields = inspectorTaskDescriptor.getFormFields();

        final boolean containsRepoField = formFields.stream().anyMatch(field -> RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID.equals(field.getId()));

        Assert.assertEquals(5, formFields.size());
        Assert.assertTrue(containsRepoField);
    }
}
