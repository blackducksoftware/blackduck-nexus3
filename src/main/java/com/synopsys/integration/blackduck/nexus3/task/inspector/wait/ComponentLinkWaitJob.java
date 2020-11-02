package com.synopsys.integration.blackduck.nexus3.task.inspector.wait;

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.wait.WaitJobTask;

public class ComponentLinkWaitJob implements WaitJobTask {
    private final String projectVersionViewHref;
    private final BlackDuckService blackDuckService;

    public ComponentLinkWaitJob(String projectVersionViewHref, BlackDuckService blackDuckService) {
        this.projectVersionViewHref = projectVersionViewHref;
        this.blackDuckService = blackDuckService;
    }

    @Override
    public boolean isComplete() throws IntegrationException {
        ProjectVersionView projectVersionView = getProjectVersionView(projectVersionViewHref);
        return projectVersionHasComponentLink(projectVersionView);
    }

    private ProjectVersionView getProjectVersionView(String projectVersionViewHref) throws IntegrationException {
        return blackDuckService.getResponse(projectVersionViewHref, ProjectVersionView.class);
    }

    private boolean projectVersionHasComponentLink(ProjectVersionView projectVersionView) {
        return projectVersionView.hasLink(ProjectVersionView.COMPONENTS_LINK);
    }

}
