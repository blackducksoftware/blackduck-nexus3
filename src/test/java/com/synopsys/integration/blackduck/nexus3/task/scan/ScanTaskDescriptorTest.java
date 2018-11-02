package com.synopsys.integration.blackduck.nexus3.task.scan;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.scheduling.Task;

public class ScanTaskDescriptorTest {

    @Test
    public void nameTest() {
        final ScanTaskDescriptor scanTaskDescriptor = new ScanTaskDescriptor();
        final String name = scanTaskDescriptor.getName();

        Assert.assertEquals(ScanTaskDescriptor.BLACK_DUCK_SCAN_TASK_NAME, name);
    }

    @Test
    public void idTest() {
        final ScanTaskDescriptor scanTaskDescriptor = new ScanTaskDescriptor();
        final String id = scanTaskDescriptor.getId();

        Assert.assertEquals(ScanTaskDescriptor.BLACK_DUCK_SCAN_TASK_ID, id);
    }

    @Test
    public void classTest() {
        final ScanTaskDescriptor scanTaskDescriptor = new ScanTaskDescriptor();
        final Class<? extends Task> taskType = scanTaskDescriptor.getType();

        Assert.assertEquals(ScanTask.class, taskType);
    }

    @Test
    public void fieldsTest() {
        final ScanTaskDescriptor scanTaskDescriptor = new ScanTaskDescriptor();
        final List<FormField> formFields = scanTaskDescriptor.getFormFields();

        final boolean containsRepoField = formFields.stream().anyMatch(field -> RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID.equals(field.getId()));

        Assert.assertEquals(8, formFields.size());
        Assert.assertTrue(containsRepoField);
    }
}
