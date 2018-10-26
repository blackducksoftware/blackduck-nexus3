package com.synopsys.integration.blackduck.nexus3.task;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import com.synopsys.integration.blackduck.nexus3.task.common.CommonTaskFilters;

public class CommonTaskFiltersTest {

    @Test
    public void doesExtensionMatchTest() {
        final CommonTaskFilters commonRepositoryTaskHelper = new CommonTaskFilters(null, null);

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

    @Test
    public void doesRepositoryPathMatchTest() {
        CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, null);

        String assetPath = "org/test/path/mything.jar";
        String workingRegexPattern = "org\\/.*";
        String brokenRegexPattern = "org\\/";
        String emptyRegexPattern = "";

        Assert.assertTrue(commonTaskFilters.doesRepositoryPathMatch(assetPath, workingRegexPattern));
        Assert.assertFalse(commonTaskFilters.doesRepositoryPathMatch(assetPath, brokenRegexPattern));
        Assert.assertTrue(commonTaskFilters.doesRepositoryPathMatch(assetPath, emptyRegexPattern));
    }

    @Test
    public void isAssetTooOldTest() {
        DateTimeParser dateTimeParser = new DateTimeParser();
        CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, dateTimeParser);

        DateTime now = new DateTime();
        DateTime dayOlder = now.minusDays(1);
        DateTime dayNewer = now.plusDays(1);

        Assert.assertTrue(commonTaskFilters.isAssetTooOld(now, dayOlder));
        Assert.assertFalse(commonTaskFilters.isAssetTooOld(now, dayNewer));
    }

    @Test
    public void hasAssetBeenModifiedTest() {
        DateTimeParser dateTimeParser = new DateTimeParser();
        CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, dateTimeParser);
    }
}
