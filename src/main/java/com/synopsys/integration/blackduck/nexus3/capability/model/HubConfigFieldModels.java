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

import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.PasswordFormField;
import org.sonatype.nexus.formfields.StringTextFormField;

public class HubConfigFieldModels {
    public static final String DEFAULT_HUB_TIMEOUT = "300";

    public static final String DESCRIPTION_HUB_IMPORT_CERT = "Import the SSL Certificates from the specified HTTPS Hub Server. Note: For this to work, the keystore must be writable by the nexus user";
    public static final String DESCRIPTION_HUB_PASSWORD = "Provide the password to authenticate with your Hub server";
    public static final String DESCRIPTION_HUB_TIMEOUT = "The timeout in seconds for a request to the Blackduck Hub server";
    public static final String DESCRIPTION_HUB_SERVER_URL = "Provide the URL that lets you access your Hub server. For example \"https://hub.example.com/\"";
    public static final String DESCRIPTION_HUB_USERNAME = "Provide the username to authenticate with your Hub server.";
    public static final String DESCRIPTION_PROXY_HOST = "The hostname of the proxy to communicate with your Hub server";
    public static final String DESCRIPTION_PROXY_PASSWORD = "Password for your authenticated proxy";
    public static final String DESCRIPTION_PROXY_PORT = "Port to communicate with the proxy";
    public static final String DESCRIPTION_PROXY_USERNAME = "Username for your authenticated proxy";

    public static final String LABEL_CONNECTION_TIMEOUT = "Connection Timeout";
    public static final String LABEL_HUB_PASSWORD = "Hub Password";
    public static final String LABEL_HUB_SERVER_URL = "Hub Server URL";
    public static final String LABEL_HUB_USERNAME = "Hub Username";
    public static final String LABEL_IMPORT_HUB_SSL_CERTIFICATE = "Import Hub SSL Certificate";

    public static final String LABEL_PROXY_HOST = "Proxy Host";
    public static final String LABEL_PROXY_PASSWORD = "Proxy Password";
    public static final String LABEL_PROXY_PORT = "Proxy Port";
    public static final String LABEL_PROXY_USERNAME = "Proxy Username";

    public static final StringTextFormField hubUrlField = new StringTextFormField(HubConfigFields.HUB_URL.getKey(), LABEL_HUB_SERVER_URL, DESCRIPTION_HUB_SERVER_URL, FormField.MANDATORY);
    public static final StringTextFormField usernameField = new StringTextFormField(HubConfigFields.HUB_USERNAME.getKey(), LABEL_HUB_USERNAME, DESCRIPTION_HUB_USERNAME, FormField.MANDATORY);
    public static final PasswordFormField passwordField = new PasswordFormField(HubConfigFields.HUB_PASSWORD.getKey(), LABEL_HUB_PASSWORD, DESCRIPTION_HUB_PASSWORD, FormField.MANDATORY);
    public static final StringTextFormField timeoutField = new StringTextFormField(HubConfigFields.HUB_TIMEOUT.getKey(), LABEL_CONNECTION_TIMEOUT, DESCRIPTION_HUB_TIMEOUT, FormField.MANDATORY).withInitialValue(DEFAULT_HUB_TIMEOUT);
    public static final CheckboxFormField autoImportCert = new CheckboxFormField(HubConfigFields.HUB_TRUST_CERT.getKey(), LABEL_IMPORT_HUB_SSL_CERTIFICATE, DESCRIPTION_HUB_IMPORT_CERT, FormField.OPTIONAL);

    public static final StringTextFormField proxyHostField = new StringTextFormField(HubConfigFields.HUB_PROXY_HOST.getKey(), LABEL_PROXY_HOST, DESCRIPTION_PROXY_HOST, FormField.OPTIONAL);
    public static final StringTextFormField proxyPortField = new StringTextFormField(HubConfigFields.HUB_PROXY_PORT.getKey(), LABEL_PROXY_PORT, DESCRIPTION_PROXY_PORT, FormField.OPTIONAL);
    public static final StringTextFormField proxyUsernameField = new StringTextFormField(HubConfigFields.HUB_PROXY_USERNAME.getKey(), LABEL_PROXY_USERNAME, DESCRIPTION_PROXY_USERNAME, FormField.OPTIONAL);
    public static final PasswordFormField proxyPasswordField = new PasswordFormField(HubConfigFields.HUB_PROXY_PASSWORD.getKey(), LABEL_PROXY_PASSWORD, DESCRIPTION_PROXY_PASSWORD, FormField.OPTIONAL);

    public static List<FormField> getFields() {
        final List<FormField> fields = new ArrayList();
        fields.add(hubUrlField);
        fields.add(usernameField);
        fields.add(passwordField);
        fields.add(timeoutField);
        fields.add(autoImportCert);
        fields.add(proxyHostField);
        fields.add(proxyPortField);
        fields.add(proxyUsernameField);
        fields.add(proxyPasswordField);
        return fields;
    }
}
