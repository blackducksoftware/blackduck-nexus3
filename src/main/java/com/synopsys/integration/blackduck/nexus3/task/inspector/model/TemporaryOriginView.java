package com.synopsys.integration.blackduck.nexus3.task.inspector.model;

import java.util.Date;

import com.synopsys.integration.blackduck.api.core.BlackDuckView;
import com.synopsys.integration.blackduck.api.generated.enumeration.ComponentSourceType;

public class TemporaryOriginView extends BlackDuckView {
    private String originId;
    private ComponentSourceType source;
    private String originName;
    private String versionName;
    private Date releasedOn;

    public TemporaryOriginView() {
    }

    public String getOriginId() {
        return originId;
    }

    public ComponentSourceType getSource() {
        return source;
    }

    public String getOriginName() {
        return originName;
    }

    public String getVersionName() {
        return versionName;
    }

    public Date getReleasedOn() {
        return releasedOn;
    }
}
