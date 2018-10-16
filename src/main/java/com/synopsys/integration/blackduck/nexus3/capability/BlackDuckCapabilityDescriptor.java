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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capability.CapabilityDescriptorSupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.PasswordFormField;
import org.sonatype.nexus.formfields.StringTextFormField;

@Singleton
@Named(BlackDuckCapabilityDescriptor.CAPABILITY_ID)
public class BlackDuckCapabilityDescriptor extends CapabilityDescriptorSupport<BlackDuckCapabilityConfiguration> {
    public static final String CAPABILITY_ID = "blackduck.capability";
    public static final String CAPABILITY_NAME = "BlackDuck";
    public static final String CAPABILITY_DESCRIPTION = "Settings required to communicate with Black Duck.";

    public static final String DEFAULT_BLACKDUCK_TIMEOUT = "300";

    private static final String DESCRIPTION_BLACKDUCK_TRUST_CERT = "Import the SSL Certificates from the specified HTTPS BlackDuck Server. Note: For this to work, the keystore must be writable by the nexus user";
    private static final String DESCRIPTION_BLACKDUCK_TIMEOUT = "The timeout in seconds for a request to the BlackDuck server";
    private static final String DESCRIPTION_BLACKDUCK_SERVER_URL = "Provide the URL that lets you access your BlackDuck server. For example \"https://blackduck.example.com/\"";
    private static final String DESCRIPTION_BLACKDUCK_API_KEY = "Api key used to access the BlackDuck instance.";
    private static final String DESCRIPTION_PROXY_HOST = "The hostname of the proxy to communicate with your BlackDuck server";
    private static final String DESCRIPTION_PROXY_PASSWORD = "Password for your authenticated proxy";
    private static final String DESCRIPTION_PROXY_PORT = "Port to communicate with the proxy";
    private static final String DESCRIPTION_PROXY_USERNAME = "Username for your authenticated proxy";

    private static final String LABEL_CONNECTION_TIMEOUT = "Connection Timeout";
    private static final String LABEL_BLACKDUCK_SERVER_URL = "BlackDuck Server URL";
    private static final String LABEL_TRUST_BLACKDUCK_SSL_CERTIFICATE = "Trust BlackDuck SSL Certificate";
    private static final String LABEL_BLACKDUCK_API_KEY = "BlackDuck Api Key";

    private static final String LABEL_PROXY_HOST = "Proxy Host";
    private static final String LABEL_PROXY_PASSWORD = "Proxy Password";
    private static final String LABEL_PROXY_PORT = "Proxy Port";
    private static final String LABEL_PROXY_USERNAME = "Proxy Username";

    private static final StringTextFormField blackDuckUrlField = new StringTextFormField(BlackDuckConfigKeys.BLACKDUCK_URL.getKey(), LABEL_BLACKDUCK_SERVER_URL, DESCRIPTION_BLACKDUCK_SERVER_URL, FormField.MANDATORY);
    private static final StringTextFormField blackDuckApiKey = new StringTextFormField(BlackDuckConfigKeys.BLACKDUCK_API_KEY.getKey(), LABEL_BLACKDUCK_API_KEY, DESCRIPTION_BLACKDUCK_API_KEY, FormField.MANDATORY);
    private static final StringTextFormField timeoutField = new StringTextFormField(BlackDuckConfigKeys.BLACKDUCK_TIMEOUT.getKey(), LABEL_CONNECTION_TIMEOUT, DESCRIPTION_BLACKDUCK_TIMEOUT, FormField.MANDATORY)
                                                                .withInitialValue(DEFAULT_BLACKDUCK_TIMEOUT);
    private static final CheckboxFormField trustCert = new CheckboxFormField(BlackDuckConfigKeys.BLACKDUCK_TRUST_CERT.getKey(), LABEL_TRUST_BLACKDUCK_SSL_CERTIFICATE, DESCRIPTION_BLACKDUCK_TRUST_CERT, FormField.OPTIONAL);

    private static final StringTextFormField proxyHostField = new StringTextFormField(BlackDuckConfigKeys.BLACKDUCK_PROXY_HOST.getKey(), LABEL_PROXY_HOST, DESCRIPTION_PROXY_HOST, FormField.OPTIONAL);
    private static final StringTextFormField proxyPortField = new StringTextFormField(BlackDuckConfigKeys.BLACKDUCK_PROXY_PORT.getKey(), LABEL_PROXY_PORT, DESCRIPTION_PROXY_PORT, FormField.OPTIONAL);
    private static final StringTextFormField proxyUsernameField = new StringTextFormField(BlackDuckConfigKeys.BLACKDUCK_PROXY_USERNAME.getKey(), LABEL_PROXY_USERNAME, DESCRIPTION_PROXY_USERNAME, FormField.OPTIONAL);
    private static final PasswordFormField proxyPasswordField = new PasswordFormField(BlackDuckConfigKeys.BLACKDUCK_PROXY_PASSWORD.getKey(), LABEL_PROXY_PASSWORD, DESCRIPTION_PROXY_PASSWORD, FormField.OPTIONAL);

    @Override
    public CapabilityType type() {
        return CapabilityType.capabilityType(CAPABILITY_ID);
    }

    @Override
    public String name() {
        return CAPABILITY_NAME;
    }

    @Override
    public String about() {
        return CAPABILITY_DESCRIPTION;
    }

    @Override
    public List<FormField> formFields() {
        final List<FormField> fields = new ArrayList();
        fields.add(blackDuckUrlField);
        fields.add(blackDuckApiKey);
        fields.add(timeoutField);
        fields.add(trustCert);
        fields.add(proxyHostField);
        fields.add(proxyPortField);
        fields.add(proxyUsernameField);
        fields.add(proxyPasswordField);
        return fields;
    }

    @Override
    protected BlackDuckCapabilityConfiguration createConfig(final Map<String, String> properties) {
        return new BlackDuckCapabilityConfiguration(properties);
    }

    @Override
    protected void validateConfig(final Map<String, String> properties, final ValidationMode validationMode) {
        log.debug("Validation Mode: {}", validationMode.name());
        if (validationMode != ValidationMode.LOAD) {
            log.info("Validating BlackDuck credentials");
            final BlackDuckCapabilityConfiguration configuration = createConfig(properties);
            configuration.createBlackDuckServerConfig();
        }

        super.validateConfig(properties, validationMode);
    }
}
