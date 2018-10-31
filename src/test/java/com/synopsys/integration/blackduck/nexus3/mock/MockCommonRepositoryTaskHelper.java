package com.synopsys.integration.blackduck.nexus3.mock;

import com.synopsys.integration.blackduck.nexus3.database.QueryManager;
import com.synopsys.integration.blackduck.nexus3.task.DateTimeParser;
import com.synopsys.integration.blackduck.nexus3.task.common.CommonRepositoryTaskHelper;

public class MockCommonRepositoryTaskHelper extends CommonRepositoryTaskHelper {

    public MockCommonRepositoryTaskHelper(final QueryManager queryManager) {
        super(queryManager, new DateTimeParser(), new MockBlackDuckConnection());
    }
}
