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

import com.synopsys.integration.blackduck.nexus3.task.AssetWrapper;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;

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
        final CommonTaskFilters commonRepositoryTaskHelper = new CommonTaskFilters(null, null, null, null, null);

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
        final CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, null, null, null, null);

        final String assetPath = "org/test/path/mything.jar";
        final String workingRegexPattern = "org\\/.*";
        final String brokenRegexPattern = "org\\/";
        final String emptyRegexPattern = "";

        Assert.assertTrue(commonTaskFilters.doesRepositoryPathMatch(assetPath, workingRegexPattern));
        Assert.assertFalse(commonTaskFilters.doesRepositoryPathMatch(assetPath, brokenRegexPattern));
        Assert.assertTrue(commonTaskFilters.doesRepositoryPathMatch(assetPath, emptyRegexPattern));
    }

    @Test
    public void isAssetTooOldTest() {
        final DateTimeParser dateTimeParser = new DateTimeParser();
        final CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, dateTimeParser, null, null, null);

        final DateTime now = new DateTime();
        final DateTime dayOlder = now.minusDays(1);
        final DateTime dayNewer = now.plusDays(1);

        Assert.assertTrue(commonTaskFilters.isAssetTooOld(now, dayOlder));
        Assert.assertFalse(commonTaskFilters.isAssetTooOld(now, dayNewer));
    }

    @Test
    public void hasAssetBeenModifiedTest() {
        final DateTimeParser dateTimeParser = new DateTimeParser();
        final CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, dateTimeParser, null, null, null);

        final DateTime now = new DateTime();
        final DateTime dayOlder = now.minusDays(1);
        final DateTime dayNewer = now.plusDays(1);

        final AssetWrapper assetWrapper = Mockito.mock(AssetWrapper.class);
        Mockito.when(assetWrapper.getAssetLastUpdated()).thenReturn(dayOlder);
        Mockito.when(assetWrapper.getFromBlackDuckAssetPanel(Mockito.any())).thenReturn(dateTimeParser.convertFromDateToString(dayNewer));

        final boolean isNotModified = commonTaskFilters.hasAssetBeenModified(assetWrapper);
        Assert.assertFalse(isNotModified);

        Mockito.when(assetWrapper.getAssetLastUpdated()).thenReturn(dayNewer);
        Mockito.when(assetWrapper.getFromBlackDuckAssetPanel(Mockito.any())).thenReturn(dateTimeParser.convertFromDateToString(dayOlder));

        final boolean isModified = commonTaskFilters.hasAssetBeenModified(assetWrapper);
        Assert.assertTrue(isModified);

        Mockito.when(assetWrapper.getAssetLastUpdated()).thenReturn(dayNewer);
        Mockito.when(assetWrapper.getFromBlackDuckAssetPanel(Mockito.any())).thenReturn(null);

        final boolean neverProcessed = commonTaskFilters.hasAssetBeenModified(assetWrapper);
        Assert.assertTrue(neverProcessed);
    }

    @Test
    public void isProxyRepositoryTest() {
        final CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, null, hostedType, proxyType, groupType);
        final boolean isProxy = commonTaskFilters.isProxyRepository(proxyType);
        final boolean isNotProxy = commonTaskFilters.isProxyRepository(groupType);

        Assert.assertTrue(isProxy);
        Assert.assertFalse(isNotProxy);
    }

    @Test
    public void isHostedRepositoryTest() {
        final CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, null, hostedType, proxyType, groupType);
        final boolean isHosted = commonTaskFilters.isHostedRepository(hostedType);
        final boolean isNotHosted = commonTaskFilters.isHostedRepository(groupType);

        Assert.assertTrue(isHosted);
        Assert.assertFalse(isNotHosted);
    }

    @Test
    public void isGroupRepositoryTest() {
        final CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, null, hostedType, proxyType, groupType);
        final boolean isGroup = commonTaskFilters.isGroupRepository(groupType);
        final boolean isNotGroup = commonTaskFilters.isGroupRepository(hostedType);

        Assert.assertTrue(isGroup);
        Assert.assertFalse(isNotGroup);
    }

    @Test
    public void findRelevantRepositoriesTest() {
        final CommonTaskFilters commonTaskFilters = new CommonTaskFilters(null, null, hostedType, proxyType, groupType);

        final Repository repository = Mockito.mock(Repository.class);
        Mockito.when(repository.getType()).thenReturn(hostedType);

        final List<Repository> singleRepositoryReturned = commonTaskFilters.findRelevantRepositories(repository);
        Assert.assertEquals(1, singleRepositoryReturned.size());

        final Repository firstRepository = Mockito.mock(Repository.class);
        final Repository secondRepository = Mockito.mock(Repository.class);

        Mockito.when(repository.getType()).thenReturn(groupType);
        Mockito.when(repository.facet(Mockito.any())).thenReturn(groupFacet);
        Mockito.when(groupFacet.leafMembers()).thenReturn(Arrays.asList(firstRepository, secondRepository));

        final List<Repository> multipleRepositories = commonTaskFilters.findRelevantRepositories(repository);
        Assert.assertEquals(2, multipleRepositories.size());
    }

    @Test
    public void skipAssetProcessingRepositoryPathTest() {
        final CommonRepositoryTaskHelper commonRepositoryTaskHelper = Mockito.mock(CommonRepositoryTaskHelper.class);
        Mockito.when(commonRepositoryTaskHelper.getRepositoryPath(Mockito.any())).thenReturn("\\/badpath");
        final DateTime assetCutoff = new DateTime().plusDays(1);
        Mockito.when(commonRepositoryTaskHelper.getAssetCutoffDateTime(Mockito.any())).thenReturn(assetCutoff);
        Mockito.when(commonRepositoryTaskHelper.getFileExtensionPatterns(Mockito.any())).thenReturn("*.jar");

        final CommonTaskFilters commonTaskFilters = new CommonTaskFilters(commonRepositoryTaskHelper, new DateTimeParser(), null, null, null);

        final AssetWrapper assetWrapper = Mockito.mock(AssetWrapper.class);
        Mockito.when(assetWrapper.getAssetLastUpdated()).thenReturn(assetCutoff.plusDays(1));
        Mockito.when(assetWrapper.getFullPath()).thenReturn("path/to/object.jar");
        Mockito.when(assetWrapper.getFilename()).thenReturn("object.jar");

        final boolean skipProcessing = commonTaskFilters.skipAssetProcessing(assetWrapper, null);
        Assert.assertTrue(skipProcessing);

        Mockito.when(commonRepositoryTaskHelper.getRepositoryPath(Mockito.any())).thenReturn("path\\/to\\/.*");

        final boolean matchingPath = commonTaskFilters.skipAssetProcessing(assetWrapper, null);
        Assert.assertFalse(matchingPath);
    }

    @Test
    public void skipAssetProcessingAssetCutoffTest() {
        final CommonRepositoryTaskHelper commonRepositoryTaskHelper = Mockito.mock(CommonRepositoryTaskHelper.class);
        Mockito.when(commonRepositoryTaskHelper.getRepositoryPath(Mockito.any())).thenReturn("");
        final DateTime assetCutoff = new DateTime().minusDays(1);
        Mockito.when(commonRepositoryTaskHelper.getAssetCutoffDateTime(Mockito.any())).thenReturn(assetCutoff);
        Mockito.when(commonRepositoryTaskHelper.getFileExtensionPatterns(Mockito.any())).thenReturn("*.jar");

        final CommonTaskFilters commonTaskFilters = new CommonTaskFilters(commonRepositoryTaskHelper, new DateTimeParser(), null, null, null);

        final AssetWrapper assetWrapper = Mockito.mock(AssetWrapper.class);
        Mockito.when(assetWrapper.getAssetLastUpdated()).thenReturn(assetCutoff.minusDays(4));
        Mockito.when(assetWrapper.getFullPath()).thenReturn("path/to/object.jar");
        Mockito.when(assetWrapper.getFilename()).thenReturn("object.jar");

        final boolean skipProcessing = commonTaskFilters.skipAssetProcessing(assetWrapper, null);
        Assert.assertTrue(skipProcessing);

        Mockito.when(assetWrapper.getAssetLastUpdated()).thenReturn(assetCutoff);

        final boolean notCutoff = commonTaskFilters.skipAssetProcessing(assetWrapper, null);
        Assert.assertFalse(notCutoff);
    }

    @Test
    public void skipAssetProcessingFileExtensionsTest() {
        final CommonRepositoryTaskHelper commonRepositoryTaskHelper = Mockito.mock(CommonRepositoryTaskHelper.class);
        Mockito.when(commonRepositoryTaskHelper.getRepositoryPath(Mockito.any())).thenReturn("");
        final DateTime assetCutoff = new DateTime().minusDays(1);
        Mockito.when(commonRepositoryTaskHelper.getAssetCutoffDateTime(Mockito.any())).thenReturn(assetCutoff);
        Mockito.when(commonRepositoryTaskHelper.getFileExtensionPatterns(Mockito.any())).thenReturn("*.bad");

        final CommonTaskFilters commonTaskFilters = new CommonTaskFilters(commonRepositoryTaskHelper, new DateTimeParser(), null, null, null);

        final AssetWrapper assetWrapper = Mockito.mock(AssetWrapper.class);
        Mockito.when(assetWrapper.getAssetLastUpdated()).thenReturn(assetCutoff);
        Mockito.when(assetWrapper.getFullPath()).thenReturn("path/to/object.jar");
        Mockito.when(assetWrapper.getFilename()).thenReturn("object.jar");

        final boolean skipProcessing = commonTaskFilters.skipAssetProcessing(assetWrapper, null);
        Assert.assertTrue(skipProcessing);

        Mockito.when(commonRepositoryTaskHelper.getFileExtensionPatterns(Mockito.any())).thenReturn("*.bad,     *.jar");

        final boolean matchingFileExtensions = commonTaskFilters.skipAssetProcessing(assetWrapper, null);
        Assert.assertFalse(matchingFileExtensions);
    }
}
