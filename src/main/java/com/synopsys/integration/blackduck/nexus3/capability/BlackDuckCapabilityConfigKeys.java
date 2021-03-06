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
package com.synopsys.integration.blackduck.nexus3.capability;

import java.util.Arrays;
import java.util.List;

public enum BlackDuckCapabilityConfigKeys {
    BLACKDUCK_TRUST_CERT("blackduck.trust.cert"),
    BLACKDUCK_PROXY_HOST("blackduck.proxy.host"),
    BLACKDUCK_PROXY_PORT("blackduck.proxy.port"),
    BLACKDUCK_PROXY_USERNAME("blackduck.proxy.username"),
    BLACKDUCK_PROXY_PASSWORD("blackduck.proxy.password"),
    BLACKDUCK_TIMEOUT("blackduck.timeout"),
    BLACKDUCK_API_KEY("blackduck.api.key"),
    BLACKDUCK_URL("blackduck.url");

    private final String key;

    BlackDuckCapabilityConfigKeys(final String key) {
        this.key = key;
    }

    public static List<BlackDuckCapabilityConfigKeys> passwordFields() {
        return Arrays.asList(BLACKDUCK_API_KEY, BLACKDUCK_PROXY_PASSWORD);
    }

    public String getKey() {
        return key;
    }

}
