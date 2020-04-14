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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.sonatype.nexus.capability.CapabilityConfigurationSupport;

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;

public class BlackDuckCapabilityConfiguration extends CapabilityConfigurationSupport {
    private final Map<String, String> capabilitySettings;

    public BlackDuckCapabilityConfiguration(Map<String, String> capabilitySettings) {
        this.capabilitySettings = capabilitySettings;
    }

    public BlackDuckServerConfig createBlackDuckServerConfig() {
        BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = new BlackDuckServerConfigBuilder();
        blackDuckServerConfigBuilder.setUrl(capabilitySettings.get(BlackDuckCapabilityConfigKeys.BLACKDUCK_URL.getKey()));
        blackDuckServerConfigBuilder.setTimeoutInSeconds(capabilitySettings.get(BlackDuckCapabilityConfigKeys.BLACKDUCK_TIMEOUT.getKey()));
        blackDuckServerConfigBuilder.setTrustCert(capabilitySettings.get(BlackDuckCapabilityConfigKeys.BLACKDUCK_TRUST_CERT.getKey()));
        blackDuckServerConfigBuilder.setApiToken(capabilitySettings.get(BlackDuckCapabilityConfigKeys.BLACKDUCK_API_KEY.getKey()));

        String proxyHost = capabilitySettings.get(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_HOST.getKey());
        String proxyPort = capabilitySettings.get(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PORT.getKey());
        String proxyUsername = capabilitySettings.get(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_USERNAME.getKey());
        String proxyPassword = capabilitySettings.get(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PASSWORD.getKey());

        if (StringUtils.isNotBlank(proxyHost)) {
            blackDuckServerConfigBuilder.setProxyHost(proxyHost);
        }
        if (StringUtils.isNotBlank(proxyPort)) {
            blackDuckServerConfigBuilder.setProxyPort(proxyPort);
        }
        if (StringUtils.isNotBlank(proxyUsername)) {
            blackDuckServerConfigBuilder.setProxyUsername(proxyUsername);
        }
        if (StringUtils.isNotBlank(proxyPassword)) {
            blackDuckServerConfigBuilder.setProxyPassword(proxyPassword);
        }

        return blackDuckServerConfigBuilder.build();
    }

}
