package com.synopsys.integration.blackduck.nexus3.task.inspector;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import com.synopsys.integration.blackduck.nexus3.task.CommonTaskKeys;

@Named
@Singleton
public class InspectorTaskDescriptor extends TaskDescriptorSupport {
    public static final String BLACK_DUCK_INSPECTOR_TASK_ID = "blackduck.inspector";
    public static final String BLACK_DUCK_INSPECTOR_TASK_NAME = "BlackDuck - Repository Inspector";

    public static final String DEFAULT_FILE_PATTERNS_MATCHES = "*.war,*.zip,*.tar.gz,*.hpi";
    public static final String DEFAULT_WORKING_DIRECTORY = "../sonatype-work";
    public static final String DEFAULT_ARTIFACT_CUTOFF = "2016-01-01T00:00:00.000";
    public static final int DEFAULT_INSPECT_PAGE_SIZE = 100;
    public static final int MAX_INSPECT_PAGE_SIZE = 100;
    public static final int MIN_INSPECT_PAGE_SIZE = 1;

    private static final String LABEL_REPOSITORY = "Repository";
    private static final String LABEL_REPOSITORY_PATH = "Repository Path";
    private static final String LABEL_FILE_PATTERN_MATCHES = "File Pattern Matches";
    private static final String LABEL_WORKING_DIRECTORY = "Working Directory";
    private static final String LABEL_ARTIFACT_CUTOFF = "Artifact Cutoff Date";
    private static final String LABEL_INSPECTION_PAGE_SIZE = "Items to keep in memory";

    private static final String DESCRIPTION_REPO_NAME = "Type in the repository in which to run the task.";
    private static final String DESCRIPTION_REPOSITORY_PATH = "Enter a repository path to run the task in recursively (ie. \"/\" for root or \"/org/apache\"). Blank will not filter based off path";
    private static final String DESCRIPTION_INSPECT_FILE_PATTERN_MATCH = "The file pattern match wildcard to filter the artifacts inspected.";
    private static final String DESCRIPTION_TASK_WORKING_DIRECTORY = "The parent directory where the blackduck directory will be created to contain temporary data for the inspection";
    private static final String DESCRIPTION_ARTIFACT_CUTOFF = "If this is set, only artifacts with a modified date later than this will be inspected. To inspect only artifacts newer than January 01, 2016 you would use "
                                                                  + "the cutoff format of \"2016-01-01T00:00:00.000\"";
    private static final String DESCRIPTION_INSPECTION_PAGE_SIZE = "Use to limit the number of items we retrieve from the Database at one time. A maximum value of 100 and a minimum of 1 are allowed";

    private static final RepositoryCombobox FIELD_REPOSITORY = new RepositoryCombobox(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, LABEL_REPOSITORY, DESCRIPTION_REPO_NAME, FormField.MANDATORY).includingAnyOfTypes(ProxyType.NAME);
    private static final StringTextFormField FIELD_REPOSITORY_PATH = new StringTextFormField(CommonTaskKeys.REPOSITORY_PATH.getParameterKey(), LABEL_REPOSITORY_PATH, DESCRIPTION_REPOSITORY_PATH, FormField.OPTIONAL);
    private static final StringTextFormField FIELD_FILE_PATTERN = new StringTextFormField(CommonTaskKeys.FILE_PATTERNS.getParameterKey(), LABEL_FILE_PATTERN_MATCHES, DESCRIPTION_INSPECT_FILE_PATTERN_MATCH, FormField.MANDATORY)
                                                                      .withInitialValue(DEFAULT_FILE_PATTERNS_MATCHES);
    private static final StringTextFormField FIELD_WORKING_DIRECTORY = new StringTextFormField(CommonTaskKeys.WORKING_DIRECTORY.getParameterKey(), LABEL_WORKING_DIRECTORY, DESCRIPTION_TASK_WORKING_DIRECTORY, FormField.MANDATORY)
                                                                           .withInitialValue(DEFAULT_WORKING_DIRECTORY);
    private static final StringTextFormField FIELD_ARTIFACT_CUTOFF = new StringTextFormField(CommonTaskKeys.OLD_ARTIFACT_CUTOFF.getParameterKey(), LABEL_ARTIFACT_CUTOFF, DESCRIPTION_ARTIFACT_CUTOFF, FormField.OPTIONAL)
                                                                         .withInitialValue(DEFAULT_ARTIFACT_CUTOFF);
    private static final NumberTextFormField FIELD_INSPECTION_PAGE_SIZE = new NumberTextFormField(CommonTaskKeys.PAGING_SIZE.getParameterKey(), LABEL_INSPECTION_PAGE_SIZE, DESCRIPTION_INSPECTION_PAGE_SIZE, FormField.MANDATORY)
                                                                              .withInitialValue(DEFAULT_INSPECT_PAGE_SIZE).withMinimumValue(MIN_INSPECT_PAGE_SIZE).withMaximumValue(MAX_INSPECT_PAGE_SIZE);

    public InspectorTaskDescriptor() {
        super(BLACK_DUCK_INSPECTOR_TASK_ID,
            InspectorTask.class,
            BLACK_DUCK_INSPECTOR_TASK_NAME,
            VISIBLE,
            EXPOSED,
            getFields()
        );
    }

    public static FormField[] getFields() {
        final FormField[] fields = { FIELD_REPOSITORY, FIELD_REPOSITORY_PATH, FIELD_FILE_PATTERN, FIELD_WORKING_DIRECTORY, FIELD_ARTIFACT_CUTOFF, FIELD_INSPECTION_PAGE_SIZE };
        return fields;
    }
}
