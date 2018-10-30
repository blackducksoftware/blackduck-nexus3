package com.synopsys.integration.blackduck.nexus3.task;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;

import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanel;
import com.synopsys.integration.blackduck.nexus3.ui.AssetPanelLabel;

public class AssetWrapperTest extends TestSupport {

    @Mock
    Component component;

    @Mock
    Blob blob;

    @Test
    public void getComponentTest() {
        final Asset asset = new Asset();
        final QueryManager queryManager = Mockito.mock(QueryManager.class);
        Mockito.when(queryManager.getComponent(Mockito.any(), Mockito.any())).thenReturn(component);

        final AssetWrapper assetWrapper = new AssetWrapper(asset, null, queryManager);
        final Component foundComponent = assetWrapper.getComponent();

        Assert.assertNotNull(foundComponent);
    }

    @Test
    public void getBlobTest() {
        final Asset asset = new Asset();
        final QueryManager queryManager = Mockito.mock(QueryManager.class);
        Mockito.when(queryManager.getBlob(Mockito.any(), Mockito.any())).thenReturn(blob);

        final AssetWrapper assetWrapper = new AssetWrapper(asset, null, queryManager);
        final Blob foundBlob = assetWrapper.getBlob();

        Assert.assertNotNull(foundBlob);
    }

    @Test
    public void getAssetPanelTest() {
        final Asset asset = new Asset();
        final NestedAttributesMap defaultAttributesMap = new NestedAttributesMap(MetadataNodeEntityAdapter.P_ATTRIBUTES, new HashMap<>());
        asset.attributes(defaultAttributesMap);
        final AssetWrapper assetWrapper = new AssetWrapper(asset, null, null);
        final AssetPanel assetPanel = assetWrapper.getAssetPanel();

        Assert.assertNotNull(assetPanel);
    }

    @Test
    public void addToBlackDuckAssetPanelTest() {
        final Asset asset = new Asset();
        final NestedAttributesMap defaultAttributesMap = new NestedAttributesMap(MetadataNodeEntityAdapter.P_ATTRIBUTES, new HashMap<>());
        asset.attributes(defaultAttributesMap);

        final TaskStatus success = TaskStatus.SUCCESS;
        final AssetPanelLabel taskStatus = AssetPanelLabel.TASK_STATUS;

        final AssetWrapper assetWrapper = new AssetWrapper(asset, null, null);
        assetWrapper.addToBlackDuckAssetPanel(taskStatus, success.name());

        final String found = getFromBlackDuckAttributes(asset, taskStatus.getLabel());
        Assert.assertNotNull(found);
        Assert.assertEquals(success.name(), found);
    }

    @Test
    public void getFromBlackDuckAssetPanelTest() {
        final Asset asset = new Asset();
        final NestedAttributesMap defaultAttributesMap = new NestedAttributesMap(MetadataNodeEntityAdapter.P_ATTRIBUTES, new HashMap<>());
        asset.attributes(defaultAttributesMap);

        final TaskStatus success = TaskStatus.SUCCESS;
        final AssetPanelLabel taskStatus = AssetPanelLabel.TASK_STATUS;
        putToBlackDuckAttributes(asset, taskStatus.getLabel(), success.name());

        final AssetWrapper assetWrapper = new AssetWrapper(asset, null, null);
        final String found = assetWrapper.getFromBlackDuckAssetPanel(taskStatus);

        Assert.assertNotNull(found);
        Assert.assertEquals(success.name(), found);
    }

    @Test
    public void removeFromBlackDuckAssetPanelTest() {
        final Asset asset = new Asset();
        final NestedAttributesMap defaultAttributesMap = new NestedAttributesMap(MetadataNodeEntityAdapter.P_ATTRIBUTES, new HashMap<>());
        asset.attributes(defaultAttributesMap);

        final TaskStatus success = TaskStatus.SUCCESS;
        final AssetPanelLabel taskStatus = AssetPanelLabel.TASK_STATUS;
        putToBlackDuckAttributes(asset, taskStatus.getLabel(), success.name());

        final AssetWrapper assetWrapper = new AssetWrapper(asset, null, null);
        assetWrapper.removeFromBlackDuckAssetPanel(taskStatus);

        final String found = (String) asset.attributes().child(AssetPanel.BLACKDUCK_CATEGORY).get(taskStatus.getLabel());

        Assert.assertNull(found);
    }

    @Test
    public void addPendingToBlackDuckPanelTest() {
        final Asset asset = new Asset();
        final NestedAttributesMap defaultAttributesMap = new NestedAttributesMap(MetadataNodeEntityAdapter.P_ATTRIBUTES, new HashMap<>());
        asset.attributes(defaultAttributesMap);

        final AssetWrapper assetWrapper = new AssetWrapper(asset, null, null);
        final String expectedPendingMessage = "pending message";
        assetWrapper.addPendingToBlackDuckPanel(expectedPendingMessage);

        final String pendingStatus = getFromBlackDuckAttributes(asset, AssetPanelLabel.TASK_STATUS.getLabel());
        final String pendingDescription = getFromBlackDuckAttributes(asset, AssetPanelLabel.TASK_STATUS_DESCRIPTION.getLabel());

        Assert.assertEquals(TaskStatus.PENDING.name(), pendingStatus);
        Assert.assertEquals(expectedPendingMessage, pendingDescription);
    }

    @Test
    public void addSuccessToBlackDuckPanelTest() {
        final Asset asset = new Asset();
        final NestedAttributesMap defaultAttributesMap = new NestedAttributesMap(MetadataNodeEntityAdapter.P_ATTRIBUTES, new HashMap<>());
        asset.attributes(defaultAttributesMap);

        final AssetWrapper assetWrapper = new AssetWrapper(asset, null, null);
        final String expectedSuccessMessage = "success message";
        assetWrapper.addSuccessToBlackDuckPanel(expectedSuccessMessage);

        final String status = getFromBlackDuckAttributes(asset, AssetPanelLabel.TASK_STATUS.getLabel());
        final String description = getFromBlackDuckAttributes(asset, AssetPanelLabel.TASK_STATUS_DESCRIPTION.getLabel());

        Assert.assertEquals(TaskStatus.SUCCESS.name(), status);
        Assert.assertEquals(expectedSuccessMessage, description);
    }

    @Test
    public void addComponentNotFoundToBlackDuckPanelTest() {
        final Asset asset = new Asset();
        final NestedAttributesMap defaultAttributesMap = new NestedAttributesMap(MetadataNodeEntityAdapter.P_ATTRIBUTES, new HashMap<>());
        asset.attributes(defaultAttributesMap);

        final AssetWrapper assetWrapper = new AssetWrapper(asset, null, null);
        final String expectedMessage = "component not found message";
        assetWrapper.addComponentNotFoundToBlackDuckPanel(expectedMessage);

        final String status = getFromBlackDuckAttributes(asset, AssetPanelLabel.TASK_STATUS.getLabel());
        final String description = getFromBlackDuckAttributes(asset, AssetPanelLabel.TASK_STATUS_DESCRIPTION.getLabel());

        Assert.assertEquals(TaskStatus.COMPONENT_NOT_FOUND.name(), status);
        Assert.assertEquals(expectedMessage, description);
    }

    @Test
    public void addFailureToBlackDuckPanelTest() {
        final Asset asset = new Asset();
        final NestedAttributesMap defaultAttributesMap = new NestedAttributesMap(MetadataNodeEntityAdapter.P_ATTRIBUTES, new HashMap<>());
        asset.attributes(defaultAttributesMap);

        final AssetWrapper assetWrapper = new AssetWrapper(asset, null, null);
        final String expectedMessage = "failure message";
        assetWrapper.addFailureToBlackDuckPanel(expectedMessage);

        final String status = getFromBlackDuckAttributes(asset, AssetPanelLabel.TASK_STATUS.getLabel());
        final String description = getFromBlackDuckAttributes(asset, AssetPanelLabel.TASK_STATUS_DESCRIPTION.getLabel());

        Assert.assertEquals(TaskStatus.FAILURE.name(), status);
        Assert.assertEquals(expectedMessage, description);
    }

    @Test
    public void removeAllBlackDuckDataTest() {
        final Asset asset = new Asset();
        final NestedAttributesMap defaultAttributesMap = new NestedAttributesMap(MetadataNodeEntityAdapter.P_ATTRIBUTES, new HashMap<>());
        asset.attributes(defaultAttributesMap);
        final String storedValue = "value";

        for (final AssetPanelLabel assetPanelLabel : AssetPanelLabel.values()) {
            putToBlackDuckAttributes(asset, assetPanelLabel.getLabel(), storedValue);
        }

        final String found = getFromBlackDuckAttributes(asset, AssetPanelLabel.TASK_STATUS.getLabel());
        Assert.assertEquals(storedValue, found);

        final AssetWrapper assetWrapper = new AssetWrapper(asset, null, null);
        assetWrapper.removeAllBlackDuckData();

        for (final AssetPanelLabel assetPanelLabel : AssetPanelLabel.values()) {
            final String emptyItem = getFromBlackDuckAttributes(asset, assetPanelLabel.getLabel());
            Assert.assertNull(emptyItem);
        }

        final String notFound = getFromBlackDuckAttributes(asset, AssetPanelLabel.TASK_STATUS.getLabel());
        Assert.assertNotEquals(storedValue, notFound);
    }

    @Test
    public void getBlackDuckStatusTest() {
        final Asset asset = new Asset();
        final NestedAttributesMap defaultAttributesMap = new NestedAttributesMap(MetadataNodeEntityAdapter.P_ATTRIBUTES, new HashMap<>());
        asset.attributes(defaultAttributesMap);
        TaskStatus expectedStatus = TaskStatus.SUCCESS;
        AssetPanelLabel assetPanelLabel = AssetPanelLabel.TASK_STATUS;
        AssetWrapper assetWrapper = new AssetWrapper(asset, null, null);

        putToBlackDuckAttributes(asset, assetPanelLabel.getLabel(), expectedStatus.name());
        TaskStatus foundStatus = assetWrapper.getBlackDuckStatus();
        Assert.assertEquals(expectedStatus, foundStatus);

        putToBlackDuckAttributes(asset, assetPanelLabel.getLabel(), "");
        TaskStatus nullStatus = assetWrapper.getBlackDuckStatus();
        Assert.assertNull(nullStatus);

        try {
            putToBlackDuckAttributes(asset, assetPanelLabel.getLabel(), "FAKE");
            assetWrapper.getBlackDuckStatus();
            Assert.fail();
        } catch (RuntimeException e) {

        }
    }

    private String getFromBlackDuckAttributes(final Asset asset, final String key) {
        return (String) asset.attributes().child(AssetPanel.BLACKDUCK_CATEGORY).get(key);
    }

    private void putToBlackDuckAttributes(final Asset asset, final String key, final String object) {
        asset.attributes().child(AssetPanel.BLACKDUCK_CATEGORY).set(key, object);
    }
}
