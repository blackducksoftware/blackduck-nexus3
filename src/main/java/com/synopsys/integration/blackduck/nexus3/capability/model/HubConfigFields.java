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
package com.synopsys.integration.blackduck.nexus3.capability.model;

import java.util.Arrays;
import java.util.List;

public enum HubConfigFields {
    HUB_TRUST_CERT("blackduck.hub.trust.cert"),
    HUB_PASSWORD("blackduck.hub.password"),
    HUB_PROXY_HOST("blackduck.hub.proxy.host"),
    HUB_PROXY_PORT("blackduck.hub.proxy.port"),
    HUB_PROXY_USERNAME("blackduck.hub.proxy.username"),
    HUB_PROXY_PASSWORD("blackduck.hub.proxy.password"),
    HUB_SCAN_MEMORY("blackduck.hub.scan.memory"),
    HUB_TIMEOUT("blackduck.hub.timeout"),
    HUB_USERNAME("blackduck.hub.username"),
    HUB_URL("blackduck.hub.url");

    private final String key;

    HubConfigFields(final String key) {
        this.key = key;
    }

    public static List<HubConfigFields> passwordFields() {
        return Arrays.asList(HUB_PASSWORD, HUB_PROXY_PASSWORD);
    }

    public String getKey() {
        return key;
    }

}
