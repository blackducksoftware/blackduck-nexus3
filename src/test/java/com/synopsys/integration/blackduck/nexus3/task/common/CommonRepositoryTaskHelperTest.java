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
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import com.synopsys.integration.blackduck.nexus3.database.PagedResult;
import com.synopsys.integration.blackduck.nexus3.mock.MockQueryManager;
import com.synopsys.integration.blackduck.nexus3.mock.model.MockAsset;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;

public class CommonRepositoryTaskHelperTest {

    @Test
    public void getTaskMessageTest() {
        CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(null, null, null);
        final String taskName = "testTask";
        final String repoName = "testRepo";
        String taskMessage = commonRepositoryTaskHelper.getTaskMessage(taskName, repoName);

        boolean containsName = taskMessage.contains(taskName);
        boolean containsRepoName = taskMessage.contains(repoName);

        Assert.assertTrue(containsName);
        Assert.assertTrue(containsRepoName);
    }

    @Test
    public void getWorkingDirectoryTest() {
        CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(null, null, null);
        TaskConfiguration taskConfiguration = new TaskConfiguration();

        File noValueFound = commonRepositoryTaskHelper.getWorkingDirectory(taskConfiguration);
        Assert.assertNotNull(noValueFound);

        final String defaultPath = CommonDescriptorHelper.DEFAULT_WORKING_DIRECTORY;
        Assert.assertTrue(noValueFound.getAbsolutePath().contains(defaultPath));

        final String testValue = "testValue";
        taskConfiguration.setString(CommonTaskKeys.WORKING_DIRECTORY.getParameterKey(), testValue);

        File foundValue = commonRepositoryTaskHelper.getWorkingDirectory(taskConfiguration);
        Assert.assertEquals(new File(testValue), foundValue);
    }

    @Test
    public void getBlackDuckPanelPathTest() {
        CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(null, null, null);
        final String expected = "attributes.BlackDuck.scan_status";

        String statusPath = commonRepositoryTaskHelper.getBlackDuckPanelPath(AssetPanelLabel.SCAN_TASK_STATUS);
        Assert.assertEquals(expected, statusPath);

        final String wrongValue = "attrbutes.something.BlackDuck.scan_status";
        Assert.assertNotEquals(wrongValue, statusPath);

        final String expectedUrl = "attributes.BlackDuck.blackduck_url";
        String urlPath = commonRepositoryTaskHelper.getBlackDuckPanelPath(AssetPanelLabel.BLACKDUCK_URL);
        Assert.assertEquals(expectedUrl, urlPath);
    }

    @Test
    public void createPagedQueryTest() {
        CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(null, null, null);
        Query builtQuery = commonRepositoryTaskHelper.createPagedQuery(Optional.empty()).build();
        String querySuffix = builtQuery.getQuerySuffix();

        boolean hasLimitAndOrder = querySuffix.contains("ORDER BY name LIMIT");
        Assert.assertTrue(hasLimitAndOrder);

        String queryText = builtQuery.getWhere();
        final String nameQuery = "name > ";
        boolean hasNoNameParam = queryText.contains(nameQuery);
        Assert.assertFalse(hasNoNameParam);

        final String queryParamName = "queryNameParam";
        Query queryWithName = commonRepositoryTaskHelper.createPagedQuery(Optional.of(queryParamName)).build();
        String queryWithNameText = queryWithName.getWhere();

        boolean hasNameQuery = queryWithNameText.contains(nameQuery);
        Assert.assertTrue(hasNameQuery);

        Map<String, Object> queryParams = queryWithName.getParameters();
        String storedName = (String) queryParams.get("p0");

        Assert.assertNotNull(storedName);
        Assert.assertEquals(queryParamName, storedName);
    }

    @Test
    public void retrievePagedAssetsTest() {
        MockQueryManager mockQueryManager = new MockQueryManager();
        DateTime now = new DateTime();
        mockQueryManager.addAsset(new MockAsset("c", now.plusDays(1)));
        mockQueryManager.addAsset(new MockAsset("a", now.minusDays(1)));
        mockQueryManager.addAsset(new MockAsset("b", now.minusDays(3)));

        CommonRepositoryTaskHelper commonRepositoryTaskHelper = new CommonRepositoryTaskHelper(mockQueryManager, null, null);
        Query query = Mockito.mock(Query.class);
        Mockito.when(query.getWhere()).thenReturn("where");
        Mockito.when(query.getParameters()).thenReturn(Collections.emptyMap());
        Mockito.when(query.getQuerySuffix()).thenReturn("suffix");

        PagedResult<Asset> foundAssets = commonRepositoryTaskHelper.retrievePagedAssets(null, query);
        Optional<String> lastFoundName = foundAssets.getLastName();
        Iterable<Asset> assetResults = foundAssets.getTypeList();
        boolean hasResults = foundAssets.hasResults();

        int counter = 0;
        Iterator iterator = assetResults.iterator();
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
