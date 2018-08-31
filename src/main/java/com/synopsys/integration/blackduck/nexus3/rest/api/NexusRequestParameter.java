/**
 * blackduck-nexus3
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
