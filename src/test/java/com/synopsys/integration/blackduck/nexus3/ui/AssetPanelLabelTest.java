package com.synopsys.integration.blackduck.nexus3.ui;

import org.junit.Assert;
import org.junit.Test;

public class AssetPanelLabelTest {

    @Test
    public void importantAssetPanelLabelKeysTest() {
        Assert.assertEquals("status", AssetPanelLabel.TASK_STATUS.getLabel());
        Assert.assertEquals("blackduck_url", AssetPanelLabel.BLACKDUCK_URL.getLabel());
        Assert.assertEquals("processed_on", AssetPanelLabel.TASK_FINISHED_TIME.getLabel());
    }

    @Test
    public void dataAssetPabelLabelKeysTest() {
        Assert.assertEquals("status_description", AssetPanelLabel.TASK_STATUS_DESCRIPTION.getLabel());
        Assert.assertEquals("origin_id", AssetPanelLabel.ASSET_ORIGIN_ID.getLabel());
        Assert.assertEquals("policy_status_overall", AssetPanelLabel.OVERALL_POLICY_STATUS.getLabel());
        Assert.assertEquals("policy_status", AssetPanelLabel.POLICY_STATUS.getLabel());
        Assert.assertEquals("vulnerabilities", AssetPanelLabel.VULNERABILITIES.getLabel());
    }
}
