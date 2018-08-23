/*
 * Copyright (C) 2018 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.synopsys.integration.blackduck.nexus3.rest.api;

public class NexusRequestParameter {
    public static final String PARAMETER_PREFIX = "?";
    public static final String PARAMETER_SEPARATOR = "&";

    private final String name;
    private final String description;

    public static NexusRequestParameter continuationToken(final String continuationToken) {
        return new NexusRequestParameter("continuationToken", continuationToken);
    }

    public static NexusRequestParameter query(final String keyword) {
        return new NexusRequestParameter("q", keyword);
    }

    public static NexusRequestParameter repository(final String repositoryName) {
        return new NexusRequestParameter("repository", repositoryName);
    }

    public static NexusRequestParameter format(final String format) {
        return new NexusRequestParameter("format", format);
    }

    public static NexusRequestParameter group(final String componentGroup) {
        return new NexusRequestParameter("group", componentGroup);
    }

    public static NexusRequestParameter name(final String componentName) {
        return new NexusRequestParameter("name", componentName);
    }

    public static NexusRequestParameter version(final String componentVersion) {
        return new NexusRequestParameter("version", componentVersion);
    }

    private NexusRequestParameter(final String name, final String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getParameterString() {
        return String.format("%s=%s", name, description);
    }

}
