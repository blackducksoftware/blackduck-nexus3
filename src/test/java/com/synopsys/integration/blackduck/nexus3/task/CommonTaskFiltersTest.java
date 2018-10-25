package com.synopsys.integration.blackduck.nexus3.task;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.synopsys.integration.blackduck.nexus3.task.common.CommonTaskFilters;

public class CommonTaskFiltersTest {

    @Test
    public void doesExtensionMatchTest() {
        final CommonTaskFilters commonRepositoryTaskHelper = new CommonTaskFilters(null);

        final String filenameSuccess1 = "test.zip";
        final String filenameSuccess2 = "test.brb";
        final String filenameFailed = "test.fake";

        final String allowedExtensions = "*.zip, *.brb";

        final boolean success1 = commonRepositoryTaskHelper.doesExtensionMatch(filenameSuccess1, allowedExtensions);
        final boolean success2 = commonRepositoryTaskHelper.doesExtensionMatch(filenameSuccess2, allowedExtensions);
        final boolean failed = commonRepositoryTaskHelper.doesExtensionMatch(filenameFailed, allowedExtensions);

        assertTrue(success1);
        assertTrue(success2);
        assertFalse(failed);
    }
}
