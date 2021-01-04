package com.synopsys.integration.blackduck.nexus3.task.inspector.model;

import java.util.Date;

import com.synopsys.integration.blackduck.api.core.BlackDuckView;
import com.synopsys.integration.blackduck.api.generated.enumeration.ComponentSourceType;

// com.synopsys.integration.blackduck.api.generated.view.OriginView API is incorrect so Gson can not convert the response, so we need this class until the library is fixed.
public class TemporaryOriginView extends BlackDuckView {
    private String originId;
    private ComponentSourceType source;
    private String originName;
    private String versionName;
    private Date releasedOn;

    public TemporaryOriginView() {
        // This class is used for de-serialization, so we should keep this empty constructor
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
