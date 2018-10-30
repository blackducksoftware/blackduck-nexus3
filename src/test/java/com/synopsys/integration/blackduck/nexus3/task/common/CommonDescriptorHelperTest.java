package com.synopsys.integration.blackduck.nexus3.task.common;

import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

public class CommonDescriptorHelperTest {

    @Test
    public void getRepositoryFieldTest() {
        final RepositoryCombobox repositoryCombobox = CommonDescriptorHelper.getRepositoryField(HostedType.NAME);

        final String helpText = repositoryCombobox.getHelpText();
        final String label = repositoryCombobox.getLabel();
        final String disallowedTypes = repositoryCombobox.getStoreFilters().get("type");

        final String formattedDescription = String.format(CommonDescriptorHelper.DESCRIPTION_REPO_NAME, HostedType.NAME);

        Assert.assertEquals(CommonDescriptorHelper.LABEL_REPOSITORY, label);
        Assert.assertEquals(formattedDescription, helpText);

        Assert.assertTrue(disallowedTypes.contains("!" + GroupType.NAME));
        Assert.assertTrue(disallowedTypes.contains("!" + ProxyType.NAME));
        Assert.assertFalse(disallowedTypes.contains("!" + HostedType.NAME));
    }

    @Test
    public void getStringFieldTest() {
        final StringTextFormField repositoryPathField = CommonDescriptorHelper.getRepositoryPathField();
        final StringTextFormField extensionField = CommonDescriptorHelper.getFilePatternField();

        final String repositoryPathHelpText = repositoryPathField.getHelpText();
        final String extensionFieldHelpText = extensionField.getHelpText();

        final String repositoryPathLabel = repositoryPathField.getLabel();
        final String extensionFieldLabel = extensionField.getLabel();

        Assert.assertEquals(CommonDescriptorHelper.DESCRIPTION_REPOSITORY_PATH, repositoryPathHelpText);
        Assert.assertEquals(CommonDescriptorHelper.DESCRIPTION_FILE_PATTERN_MATCH, extensionFieldHelpText);

        Assert.assertEquals(CommonDescriptorHelper.LABEL_REPOSITORY_PATH, repositoryPathLabel);
        Assert.assertEquals(CommonDescriptorHelper.LABEL_FILE_PATTERN_MATCHES, extensionFieldLabel);
    }

    @Test
    public void getStringFieldWithInitialValuesTest() {
        final StringTextFormField workingDirectoryField = CommonDescriptorHelper.getWorkingDirectoryField();
        final StringTextFormField assetCutoffDateField = CommonDescriptorHelper.getAssetCutoffDateField();

        final String workingDirectoryHelpText = workingDirectoryField.getHelpText();
        final String assetCutoffDateHelpText = assetCutoffDateField.getHelpText();

        final String workingDirectoryLabel = workingDirectoryField.getLabel();
        final String assetCutoffDateLabel = assetCutoffDateField.getLabel();

        final String workingDirectoryInitialValue = workingDirectoryField.getInitialValue();
        final String assetCutoffDateInitialValue = assetCutoffDateField.getInitialValue();

        Assert.assertEquals(CommonDescriptorHelper.DESCRIPTION_TASK_WORKING_DIRECTORY, workingDirectoryHelpText);
        Assert.assertEquals(CommonDescriptorHelper.DESCRIPTION_ASSET_CUTOFF, assetCutoffDateHelpText);

        Assert.assertEquals(CommonDescriptorHelper.LABEL_WORKING_DIRECTORY, workingDirectoryLabel);
        Assert.assertEquals(CommonDescriptorHelper.LABEL_ASSET_CUTOFF, assetCutoffDateLabel);

        Assert.assertEquals(CommonDescriptorHelper.DEFAULT_WORKING_DIRECTORY, workingDirectoryInitialValue);
        Assert.assertEquals(CommonDescriptorHelper.DEFAULT_ARTIFACT_CUTOFF, assetCutoffDateInitialValue);
    }
}
