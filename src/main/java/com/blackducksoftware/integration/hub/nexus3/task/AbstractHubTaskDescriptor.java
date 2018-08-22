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
package com.blackducksoftware.integration.hub.nexus3.task;

import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.PasswordFormField;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.scheduling.TaskDescriptor;

import com.blackducksoftware.integration.hub.nexus3.config.HubServerField;

public abstract class AbstractHubTaskDescriptor implements TaskDescriptor {
    public static final String DEFAULT_HUB_TIMEOUT = "300";
    private static final String DESCRIPTION_HUB_IMPORT_CERT = "Import the SSL Certificates from the specified HTTPS Hub Server. Note: For this to work, the keystore must be writable by the nexus user";
    private static final String DESCRIPTION_HUB_PASSWORD = "Provide the password to authenticate with your Hub server";
    private static final String DESCRIPTION_HUB_TIMEOUT = "The timeout in seconds for a request to the Blackduck Hub server";
    private static final String DESCRIPTION_HUB_URL = "Provide the URL that lets you access your Hub server. For example \"https://hub.example.com/\"";
    private static final String DESCRIPTION_HUB_USERNAME = "Provide the username to authenticate with your Hub server.";
    private static final String DESCRIPTION_PROXY_HOST = "The hostname of the proxy to communicate with your Hub server";
    private static final String DESCRIPTION_PROXY_PASSWORD = "Password for your authenticated proxy";
    private static final String DESCRIPTION_PROXY_PORT = "Port to communicate with the proxy";
    private static final String DESCRIPTION_PROXY_USERNAME = "Username for your authenticated proxy";

    private static final String LABEL_CONNECTION_TIMEOUT = "Connection Timeout";
    private static final String LABEL_HUB_PASSWORD = "Hub Password";
    private static final String LABEL_HUB_SERVER_URL = "Hub Server URL";
    private static final String LABEL_HUB_USERNAME = "Hub Username";
    private static final String LABEL_IMPORT_HUB_SSL_CERTIFICATE = "Import Hub SSL Certificate";

    private static final String LABEL_PROXY_HOST = "Proxy Host";
    private static final String LABEL_PROXY_PASSWORD = "Proxy Password";
    private static final String LABEL_PROXY_PORT = "Proxy Port";
    private static final String LABEL_PROXY_USERNAME = "Proxy Username";

    private final StringTextFormField hubUrlField = new StringTextFormField(HubServerField.HUB_URL.getKey(), LABEL_HUB_SERVER_URL, DESCRIPTION_HUB_URL, FormField.MANDATORY);
    private final StringTextFormField usernameField = new StringTextFormField(HubServerField.HUB_USERNAME.getKey(), LABEL_HUB_USERNAME, DESCRIPTION_HUB_USERNAME, FormField.MANDATORY);
    private final PasswordFormField passwordField = new PasswordFormField(HubServerField.HUB_PASSWORD.getKey(), LABEL_HUB_PASSWORD, DESCRIPTION_HUB_PASSWORD, FormField.MANDATORY);
    private final StringTextFormField timeoutField = new StringTextFormField(HubServerField.HUB_TIMEOUT.getKey(), LABEL_CONNECTION_TIMEOUT, DESCRIPTION_HUB_TIMEOUT, FormField.OPTIONAL).withInitialValue(DEFAULT_HUB_TIMEOUT);
    private final CheckboxFormField autoImportCert = new CheckboxFormField(HubServerField.HUB_TRUST_CERT.getKey(), LABEL_IMPORT_HUB_SSL_CERTIFICATE, DESCRIPTION_HUB_IMPORT_CERT, FormField.OPTIONAL);

    private final StringTextFormField proxyHostField = new StringTextFormField(HubServerField.HUB_PROXY_HOST.getKey(), LABEL_PROXY_HOST, DESCRIPTION_PROXY_HOST, FormField.OPTIONAL);
    private final StringTextFormField proxyPortField = new StringTextFormField(HubServerField.HUB_PROXY_PORT.getKey(), LABEL_PROXY_PORT, DESCRIPTION_PROXY_PORT, FormField.OPTIONAL);
    private final StringTextFormField proxyUsernameField = new StringTextFormField(HubServerField.HUB_PROXY_USERNAME.getKey(), LABEL_PROXY_USERNAME, DESCRIPTION_PROXY_USERNAME, FormField.OPTIONAL);
    private final PasswordFormField proxyPasswordField = new PasswordFormField(HubServerField.HUB_PROXY_PASSWORD.getKey(), LABEL_PROXY_PASSWORD, DESCRIPTION_PROXY_PASSWORD, FormField.OPTIONAL);

    @Override
    @SuppressWarnings("rawtypes")
    // @formatter:off
    public List<FormField> getFormFields() {
        return Arrays.asList(
                hubUrlField,
                usernameField,
                passwordField,
                timeoutField,
                autoImportCert,
                proxyHostField,
                proxyPortField,
                proxyUsernameField,
                proxyPasswordField
                );
    }
    // @formatter:on

    @Override
    public boolean isVisible() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isExposed() {
        // TODO Auto-generated method stub
        return false;
    }

}
