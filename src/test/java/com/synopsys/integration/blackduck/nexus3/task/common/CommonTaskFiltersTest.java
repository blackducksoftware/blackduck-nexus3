package com.synopsys.integration.blackduck.nexus3.task.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.exception.IntegrationException;

public class CommonTaskFiltersTest extends TestSupport {

    @Mock
    Type groupType;

    @Mock
    Type hostedType;

    @Mock
    Type proxyType;

    @Mock
    GroupFacet groupFacet;

    @Test
    public void doesExtensionMatchTest() {
        CommonTaskFilters commonRepositoryTaskHelper = new CommonTaskFilters(null, null, null, null);

        final String filenameSuccess1 = "test.zip";
        final String filenameSuccess2 = "test.brb";
        final String filenameFailed = "test.fake";

        final String allowedExtensions = "*.zip, *.brb";

        boolean success1 = commonRepositoryTaskHelper.doesExtensionMatch(filenameSuccess1, allowedExtensions);
        boolean success2 = commonRepositoryTaskHelper.doesExtensionMatch(filenameSuccess2, allowedExtensions);
        boolean failed = commonRepositoryTaskHelper.doesExtensionMatch(filenameFailed, allowedExtensions);

        assertTrue(success1);
        assertTrue(success2);
        assertFalse(failed);
    }

    @Test
    public void doesRepositoryPathMatchTest() {
        CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, null, null, null);

        final String assetPath = "org/test/path/mything.jar";
        final String workingRegexPattern = "org\\/.*";
        final String brokenRegexPattern = "org\\/";
        final String emptyRegexPattern = "";

        Assert.assertTrue(commonTaskFilters.doesRepositoryPathMatch(assetPath, workingRegexPattern));
        Assert.assertFalse(commonTaskFilters.doesRepositoryPathMatch(assetPath, brokenRegexPattern));
        Assert.assertTrue(commonTaskFilters.doesRepositoryPathMatch(assetPath, emptyRegexPattern));
    }

    @Test
    public void getRepositoryPathTest() {
        CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, null, null, null);
        TaskConfiguration taskConfiguration = new TaskConfiguration();

        String noValueFound = commonTaskFilters.getRepositoryPath(taskConfiguration);
        Assert.assertNull(noValueFound);

        final String testValue = "testValue";
        taskConfiguration.setString(CommonTaskKeys.REPOSITORY_PATH.getParameterKey(), testValue);

        String foundValue = commonTaskFilters.getRepositoryPath(taskConfiguration);
        Assert.assertEquals(testValue, foundValue);
    }

    @Test
    public void getFileExtensionsPathTest() {
        CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, null, null, null);
        TaskConfiguration taskConfiguration = new TaskConfiguration();

        String noValueFound = commonTaskFilters.getFileExtensionPatterns(taskConfiguration);
        Assert.assertNull(noValueFound);

        final String testValue = "testValue";
        taskConfiguration.setString(CommonTaskKeys.FILE_PATTERNS.getParameterKey(), testValue);

        String foundValue = commonTaskFilters.getFileExtensionPatterns(taskConfiguration);
        Assert.assertEquals(testValue, foundValue);
    }

    @Test
    public void getAssetCutoffDateTimeTest() {
        DateTimeParser dateTimeParser = new DateTimeParser();
        CommonTaskFilters commonTaskFilters = new CommonTaskFilters(dateTimeParser, null, null, null);
        TaskConfiguration taskConfiguration = new TaskConfiguration();

        DateTime noValueFound = commonTaskFilters.getAssetCutoffDateTime(taskConfiguration);
        Assert.assertNull(noValueFound);

        DateTime testValue = new DateTime();
        taskConfiguration.setString(CommonTaskKeys.OLD_ASSET_CUTOFF.getParameterKey(), testValue.toString(DateTimeParser.DATE_TIME_PATTERN));

        DateTime foundValue = commonTaskFilters.getAssetCutoffDateTime(taskConfiguration);
        Assert.assertEquals(testValue.toString(DateTimeParser.DATE_TIME_PATTERN), foundValue.toString(DateTimeParser.DATE_TIME_PATTERN));
    }

    @Test
    public void doesRepositoryApplyTest() {
        Repository repository = Mockito.mock(Repository.class);
        final String repoName = "testRepo";
        Mockito.when(repository.getName()).thenReturn(repoName);

        CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, null, null, null);

        boolean repoApplies = commonTaskFilters.doesRepositoryApply(repository, repoName);
        boolean repoDoesNotApply = commonTaskFilters.doesRepositoryApply(repository, "wrongName");

        Assert.assertTrue(repoApplies);
        Assert.assertFalse(repoDoesNotApply);
    }

    @Test
    public void isAssetTooOldTest() {
        DateTimeParser dateTimeParser = new DateTimeParser();
        CommonTaskFilters commonTaskFilters = new CommonTaskFilters(dateTimeParser, null, null, null);

        DateTime now = new DateTime();
        DateTime dayOlder = now.minusDays(1);
        DateTime dayNewer = now.plusDays(1);

        Assert.assertTrue(commonTaskFilters.isAssetTooOld(now, dayOlder));
        Assert.assertFalse(commonTaskFilters.isAssetTooOld(now, dayNewer));
    }

    @Test
    public void hasAssetBeenModifiedTest() {
        DateTimeParser dateTimeParser = new DateTimeParser();
        CommonTaskFilters commonTaskFilters = new CommonTaskFilters(dateTimeParser, null, null, null);

        DateTime now = new DateTime();
        DateTime dayOlder = now.minusDays(1);
        DateTime dayNewer = now.plusDays(1);

        AssetWrapper assetWrapper = Mockito.mock(AssetWrapper.class);
        Mockito.when(assetWrapper.getAssetLastUpdated()).thenReturn(dayOlder);
        Mockito.when(assetWrapper.getFromBlackDuckAssetPanel(Mockito.any())).thenReturn(dateTimeParser.convertFromDateToString(dayNewer));

        boolean isNotModified = commonTaskFilters.hasAssetBeenModified(assetWrapper);
        Assert.assertFalse(isNotModified);

        Mockito.when(assetWrapper.getAssetLastUpdated()).thenReturn(dayNewer);
        Mockito.when(assetWrapper.getFromBlackDuckAssetPanel(Mockito.any())).thenReturn(dateTimeParser.convertFromDateToString(dayOlder));

        boolean isModified = commonTaskFilters.hasAssetBeenModified(assetWrapper);
        Assert.assertTrue(isModified);

        Mockito.when(assetWrapper.getAssetLastUpdated()).thenReturn(dayNewer);
        Mockito.when(assetWrapper.getFromBlackDuckAssetPanel(Mockito.any())).thenReturn(null);

        boolean neverProcessed = commonTaskFilters.hasAssetBeenModified(assetWrapper);
        Assert.assertTrue(neverProcessed);
    }

    @Test
    public void isProxyRepositoryTest() {
        CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, hostedType, proxyType, groupType);
        boolean isProxy = commonTaskFilters.isProxyRepository(proxyType);
        boolean isNotProxy = commonTaskFilters.isProxyRepository(groupType);

        Assert.assertTrue(isProxy);
        Assert.assertFalse(isNotProxy);
    }

    @Test
    public void isHostedRepositoryTest() {
        CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, hostedType, proxyType, groupType);
        boolean isHosted = commonTaskFilters.isHostedRepository(hostedType);
        boolean isNotHosted = commonTaskFilters.isHostedRepository(groupType);

        Assert.assertTrue(isHosted);
        Assert.assertFalse(isNotHosted);
    }

    @Test
    public void isGroupRepositoryTest() {
        CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, hostedType, proxyType, groupType);
        boolean isGroup = commonTaskFilters.isGroupRepository(groupType);
        boolean isNotGroup = commonTaskFilters.isGroupRepository(hostedType);

        Assert.assertTrue(isGroup);
        Assert.assertFalse(isNotGroup);
    }

    @Test
    public void findRelevantRepositoriesTest() {
        CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, hostedType, proxyType, groupType);

        Repository repository = Mockito.mock(Repository.class);
        Mockito.when(repository.getType()).thenReturn(hostedType);

        List<Repository> singleRepositoryReturned = commonTaskFilters.findRelevantRepositories(repository);
        Assert.assertEquals(1, singleRepositoryReturned.size());

        Repository firstRepository = Mockito.mock(Repository.class);
        Repository secondRepository = Mockito.mock(Repository.class);

        Mockito.when(repository.getType()).thenReturn(groupType);
        Mockito.when(repository.facet(Mockito.any())).thenReturn(groupFacet);
        Mockito.when(groupFacet.leafMembers()).thenReturn(Arrays.asList(firstRepository, secondRepository));

        List<Repository> multipleRepositories = commonTaskFilters.findRelevantRepositories(repository);
        Assert.assertEquals(2, multipleRepositories.size());
    }

    @Test
    public void doesAssetPathAndExtensionMatchRepositoryPathTest() throws IntegrationException {
        TaskConfiguration taskConfiguration = Mockito.mock(TaskConfiguration.class);
        Mockito.when(taskConfiguration.getString(Mockito.eq(CommonTaskKeys.REPOSITORY_PATH.getParameterKey()))).thenReturn("\\/badpath");
        Mockito.when(taskConfiguration.getString(Mockito.eq(CommonTaskKeys.FILE_PATTERNS.getParameterKey()))).thenReturn("*.jar");
        DateTimeParser dateTimeParser = new DateTimeParser();
        DateTime assetCutoff = new DateTime().minusDays(1);
        Mockito.when(taskConfiguration.getString(Mockito.eq(CommonTaskKeys.OLD_ASSET_CUTOFF.getParameterKey()))).thenReturn(dateTimeParser.convertFromDateToString(assetCutoff));

        CommonTaskFilters commonTaskFilters = new CommonTaskFilters(dateTimeParser, null, null, null);

        boolean badPathMatch = commonTaskFilters.doesAssetPathAndExtensionMatch("path/to/object.jar", "object.jar", taskConfiguration);
        Assert.assertFalse(badPathMatch);

        Mockito.when(taskConfiguration.getString(Mockito.eq(CommonTaskKeys.REPOSITORY_PATH.getParameterKey()))).thenReturn("path\\/to\\/.*");

        boolean matchingPath = commonTaskFilters.doesAssetPathAndExtensionMatch("path/to/object.jar", "object.jar", taskConfiguration);
        Assert.assertTrue(matchingPath);
    }

    @Test
    public void isAssetTooOldForTaskAssetCutoffTest() {
        TaskConfiguration taskConfiguration = Mockito.mock(TaskConfiguration.class);
        Mockito.when(taskConfiguration.getString(Mockito.eq(CommonTaskKeys.REPOSITORY_PATH.getParameterKey()))).thenReturn("");
        Mockito.when(taskConfiguration.getString(Mockito.eq(CommonTaskKeys.FILE_PATTERNS.getParameterKey()))).thenReturn("*.jar");
        DateTimeParser dateTimeParser = new DateTimeParser();
        DateTime assetCutoff = new DateTime().minusDays(1);
        Mockito.when(taskConfiguration.getString(Mockito.eq(CommonTaskKeys.OLD_ASSET_CUTOFF.getParameterKey()))).thenReturn(dateTimeParser.convertFromDateToString(assetCutoff));

        CommonTaskFilters commonTaskFilters = new CommonTaskFilters(dateTimeParser, null, null, null);

        boolean skipProcessing = commonTaskFilters.isAssetTooOldForTask(assetCutoff.minusDays(4), taskConfiguration);
        Assert.assertTrue(skipProcessing);

        boolean notCutoff = commonTaskFilters.isAssetTooOldForTask(assetCutoff, taskConfiguration);
        Assert.assertFalse(notCutoff);
    }

    @Test
    public void doesAssetPathAndExtensionMatchFileExtensionsTest() {
        TaskConfiguration taskConfiguration = Mockito.mock(TaskConfiguration.class);
        Mockito.when(taskConfiguration.getString(Mockito.eq(CommonTaskKeys.REPOSITORY_PATH.getParameterKey()))).thenReturn("");
        Mockito.when(taskConfiguration.getString(Mockito.eq(CommonTaskKeys.FILE_PATTERNS.getParameterKey()))).thenReturn("*.bad");
        DateTimeParser dateTimeParser = new DateTimeParser();
        DateTime assetCutoff = new DateTime().minusDays(1);
        Mockito.when(taskConfiguration.getString(Mockito.eq(CommonTaskKeys.OLD_ASSET_CUTOFF.getParameterKey()))).thenReturn(dateTimeParser.convertFromDateToString(assetCutoff));

        CommonTaskFilters commonTaskFilters = new CommonTaskFilters(dateTimeParser, null, null, null);

        boolean badExtensionMatch = commonTaskFilters.doesAssetPathAndExtensionMatch("path/to/object.jar", "object.jar", taskConfiguration);
        Assert.assertFalse(badExtensionMatch);

        Mockito.when(taskConfiguration.getString(Mockito.eq(CommonTaskKeys.FILE_PATTERNS.getParameterKey()))).thenReturn("*.bad,     *.jar");

        boolean matchingFileExtensions = commonTaskFilters.doesAssetPathAndExtensionMatch("path/to/object.jar", "object.jar", taskConfiguration);
        Assert.assertTrue(matchingFileExtensions);
    }
}
