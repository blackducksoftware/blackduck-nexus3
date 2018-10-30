package com.synopsys.integration.blackduck.nexus3.task;

import org.junit.Assert;
import org.junit.Test;

public class TaskStatusTest {

    @Test
    public void forceValidNamesTest() {
        final TaskStatus successStatus = TaskStatus.SUCCESS;
        final TaskStatus pendingStatus = TaskStatus.PENDING;
        final TaskStatus failureStatus = TaskStatus.FAILURE;
        final TaskStatus componentNotFoundStatus = TaskStatus.COMPONENT_NOT_FOUND;

        Assert.assertEquals("SUCCESS", successStatus.name());
        Assert.assertEquals("PENDING", pendingStatus.name());
        Assert.assertEquals("FAILURE", failureStatus.name());
        Assert.assertEquals("COMPONENT_NOT_FOUND", componentNotFoundStatus.name());
    }
}
