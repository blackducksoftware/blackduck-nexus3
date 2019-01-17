package com.synopsys.integration.blackduck.nexus3.task.common;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.mock.MockQueryManager;
import com.synopsys.integration.blackduck.nexus3.mock.model.MockAsset;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;

public class CommonRepositoryTaskHelperTest {

    @Test
    public void doesRepositoryApplyTest() {
        final Repository repository = Mockito.mock(Repository.class);
        final String repoName = "testRepo";
        Mockito.when(repository.getName()).thenReturn(repoName);

        final CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(null, null, null);
        final boolean repoApplies = commonRepositoryTaskHelper.doesRepositoryApply(repository, repoName);
        final boolean repoDoesNotApply = commonRepositoryTaskHelper.doesRepositoryApply(repository, "wrongName");

        Assert.assertTrue(repoApplies);
        Assert.assertFalse(repoDoesNotApply);
    }

    @Test
    public void getTaskMessageTest() {
        final CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(null, null, null);
        final String taskName = "testTask";
        final String repoName = "testRepo";
        final String taskMessage = commonRepositoryTaskHelper.getTaskMessage(taskName, repoName);

        final boolean containsName = taskMessage.contains(taskName);
        final boolean containsRepoName = taskMessage.contains(repoName);

        Assert.assertTrue(containsName);
        Assert.assertTrue(containsRepoName);
    }

    @Test
    public void getRepositoryPathTest() {
        final CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(null, null, null);
        final TaskConfiguration taskConfiguration = new TaskConfiguration();

        final String noValueFound = commonRepositoryTaskHelper.getRepositoryPath(taskConfiguration);
        Assert.assertNull(noValueFound);

        final String testValue = "testValue";
        taskConfiguration.setString(CommonTaskKeys.REPOSITORY_PATH.getParameterKey(), testValue);

        final String foundValue = commonRepositoryTaskHelper.getRepositoryPath(taskConfiguration);
        Assert.assertEquals(testValue, foundValue);
    }

    @Test
    public void getFileExtensionsPathTest() {
        final CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(null, null, null);
        final TaskConfiguration taskConfiguration = new TaskConfiguration();

        final String noValueFound = commonRepositoryTaskHelper.getFileExtensionPatterns(taskConfiguration);
        Assert.assertNull(noValueFound);

        final String testValue = "testValue";
        taskConfiguration.setString(CommonTaskKeys.FILE_PATTERNS.getParameterKey(), testValue);

        final String foundValue = commonRepositoryTaskHelper.getFileExtensionPatterns(taskConfiguration);
        Assert.assertEquals(testValue, foundValue);
    }

    @Test
    public void getAssetCutoffDateTimeTest() {
        final DateTimeParser dateTimeParser = new DateTimeParser();
        final CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(null, dateTimeParser, null);
        final TaskConfiguration taskConfiguration = new TaskConfiguration();

        final DateTime noValueFound = commonRepositoryTaskHelper.getAssetCutoffDateTime(taskConfiguration);
        Assert.assertNull(noValueFound);

        final DateTime testValue = new DateTime();
        taskConfiguration.setString(CommonTaskKeys.OLD_ASSET_CUTOFF.getParameterKey(), testValue.toString(DateTimeParser.DATE_TIME_PATTERN));

        final DateTime foundValue = commonRepositoryTaskHelper.getAssetCutoffDateTime(taskConfiguration);
        Assert.assertEquals(testValue.toString(DateTimeParser.DATE_TIME_PATTERN), foundValue.toString(DateTimeParser.DATE_TIME_PATTERN));
    }

    @Test
    public void getWorkingDirectoryTest() {
        final CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(null, null, null);
        final TaskConfiguration taskConfiguration = new TaskConfiguration();

        final File noValueFound = commonRepositoryTaskHelper.getWorkingDirectory(taskConfiguration);
        Assert.assertNotNull(noValueFound);

        final String defaultPath = CommonDescriptorHelper.DEFAULT_WORKING_DIRECTORY;
        Assert.assertTrue(noValueFound.getAbsolutePath().contains(defaultPath));

        final String testValue = "testValue";
        taskConfiguration.setString(CommonTaskKeys.WORKING_DIRECTORY.getParameterKey(), testValue);

        final File foundValue = commonRepositoryTaskHelper.getWorkingDirectory(taskConfiguration);
        Assert.assertEquals(new File(testValue), foundValue);
    }

    @Test
    public void getBlackDuckPanelPathTest() {
        final CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(null, null, null);
        final String expected = "attributes.BlackDuck.scan_status";

        final String statusPath = commonRepositoryTaskHelper.getBlackDuckPanelPath(AssetPanelLabel.SCAN_TASK_STATUS);
        Assert.assertEquals(expected, statusPath);

        final String wrongValue = "attrbutes.something.BlackDuck.scan_status";
        Assert.assertNotEquals(wrongValue, statusPath);

        final String expectedUrl = "attributes.BlackDuck.blackduck_url";
        final String urlPath = commonRepositoryTaskHelper.getBlackDuckPanelPath(AssetPanelLabel.BLACKDUCK_URL);
        Assert.assertEquals(expectedUrl, urlPath);
    }

    @Test
    public void createPagedQueryTest() {
        final CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(null, null, null);
        final Query builtQuery = commonRepositoryTaskHelper.createPagedQuery(Optional.empty()).build();
        final String querySuffix = builtQuery.getQuerySuffix();

        final boolean hasLimitAndOrder = querySuffix.contains("ORDER BY name LIMIT");
        Assert.assertTrue(hasLimitAndOrder);

        final String queryText = builtQuery.getWhere();
        final String nameQuery = "name > ";
        final boolean hasNoNameParam = queryText.contains(nameQuery);
        Assert.assertFalse(hasNoNameParam);

        final String queryParamName = "queryNameParam";
        final Query queryWithName = commonRepositoryTaskHelper.createPagedQuery(Optional.of(queryParamName)).build();
        final String queryWithNameText = queryWithName.getWhere();

        final boolean hasNameQuery = queryWithNameText.contains(nameQuery);
        Assert.assertTrue(hasNameQuery);

        final Map<String, Object> queryParams = queryWithName.getParameters();
        final String storedName = (String) queryParams.get("p0");

        Assert.assertNotNull(storedName);
        Assert.assertEquals(queryParamName, storedName);
    }

    @Test
    public void retrievePagedAssetsTest() {
        final MockQueryManager mockQueryManager = new MockQueryManager();
        final DateTime now = new DateTime();
        mockQueryManager.addAsset(new MockAsset("c", now.plusDays(1)));
        mockQueryManager.addAsset(new MockAsset("a", now.minusDays(1)));
        mockQueryManager.addAsset(new MockAsset("b", now.minusDays(3)));

        final CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(mockQueryManager, null, null);
        final Query query = Mockito.mock(Query.class);
        Mockito.when(query.getWhere()).thenReturn("where");
        Mockito.when(query.getParameters()).thenReturn(Collections.emptyMap());
        Mockito.when(query.getQuerySuffix()).thenReturn("suffix");

        final PagedResult<Asset> foundAssets = commonRepositoryTaskHelper.retrievePagedAssets(null, query);
        final Optional<String> lastFoundName = foundAssets.getLastName();
        final Iterable<Asset> assetResults = foundAssets.getTypeList();
        final boolean hasResults = foundAssets.hasResults();

        int counter = 0;
        final Iterator iterator = assetResults.iterator();
        while (iterator.hasNext()) {
            counter++;
            iterator.next();
        }

        Assert.assertTrue(hasResults);
        Assert.assertTrue(lastFoundName.isPresent());
        Assert.assertEquals("b", lastFoundName.get());
        Assert.assertEquals(3, counter);
    }
}
