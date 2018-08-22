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
package com.blackducksoftware.integration.hub.nexus3.config;

import java.util.Arrays;
import java.util.List;

public enum HubServerField {
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

    private String key;

    private HubServerField(final String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    public static List<HubServerField> passwordFields() {
        return Arrays.asList(HUB_PASSWORD, HUB_PROXY_PASSWORD);
    }

}
