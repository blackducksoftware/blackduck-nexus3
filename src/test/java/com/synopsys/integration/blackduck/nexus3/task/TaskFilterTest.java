package com.synopsys.integration.blackduck.nexus3.task;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TaskFilterTest {

    @Test
    public void doesExtensionMatchTest() {
        final TaskFilter taskFilter = new TaskFilter();

        final String filenameSuccess1 = "test.zip";
        final String filenameSuccess2 = "test.brb";
        final String filenameFailed = "test.fake";

        final String allowedExtensions = "*.zip, *.brb";

        final boolean success1 = taskFilter.doesExtensionMatch(filenameSuccess1, allowedExtensions);
        final boolean success2 = taskFilter.doesExtensionMatch(filenameSuccess2, allowedExtensions);
        final boolean failed = taskFilter.doesExtensionMatch(filenameFailed, allowedExtensions);

        assertTrue(success1);
        assertTrue(success2);
        assertFalse(failed);
    }
}
