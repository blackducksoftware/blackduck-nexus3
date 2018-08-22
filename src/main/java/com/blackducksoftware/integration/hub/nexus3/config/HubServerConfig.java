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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.encryption.EncryptionUtils;
import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.exception.IntegrationException;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Named
@Singleton
public class HubServerConfig {
    public static final String HUB_SERVER_CONFIG_DIRECTORY_PATH = "tools/blackduck";
    public static final String HUB_SERVER_CONFIG_FILE_NAME = "serverConfig.json";

    private final EncryptionUtils encryptor = new EncryptionUtils();
    private final String baseDirectoryName;
    private final JsonParser jsonParser;
    private final Gson gson;

    // FIXME these have to be injectable
    @Inject
    public HubServerConfig(final String baseDirectoryName, final JsonParser jsonParser, final Gson gson) {
        this.baseDirectoryName = baseDirectoryName;
        this.jsonParser = jsonParser;
        this.gson = gson;
    }

    public Map<String, String> getConfigMapAsPlainText() throws IntegrationException {
        final Map<HubServerField, String> configMap = getConfigMap();
        return configMap.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getKey(), e -> e.getValue()));
    }

    public Map<HubServerField, String> getConfigMap() throws IntegrationException {
        final Map<HubServerField, String> configMap = new HashMap<>();

        String configFileContents = null;
        try {
            final File configFile = getConfigFile();
            configFileContents = FileUtils.readFileToString(configFile);
        } catch (final IOException e) {
            throw new IntegrationException(e);
        }
        if (StringUtils.isNotBlank(configFileContents)) {
            final JsonObject configJson = (JsonObject) jsonParser.parse(configFileContents);
            final JsonElement hubUrl = configJson.get(HubServerField.HUB_URL.getKey());
            final JsonElement hubUsername = configJson.get(HubServerField.HUB_USERNAME.getKey());
            final JsonElement hubPassword = configJson.get(HubServerField.HUB_PASSWORD.getKey());
            final JsonElement hubTimeout = configJson.get(HubServerField.HUB_TIMEOUT.getKey());
            final JsonElement hubTrustCert = configJson.get(HubServerField.HUB_TRUST_CERT.getKey());

            final JsonElement hubProxyHost = configJson.get(HubServerField.HUB_PROXY_HOST.getKey());
            final JsonElement hubProxyPort = configJson.get(HubServerField.HUB_PROXY_PORT.getKey());
            final JsonElement hubProxyUsername = configJson.get(HubServerField.HUB_PROXY_USERNAME.getKey());
            final JsonElement hubProxyPassword = configJson.get(HubServerField.HUB_PROXY_PASSWORD.getKey());

            configMap.put(HubServerField.HUB_URL, hubUrl.getAsString());
            configMap.put(HubServerField.HUB_USERNAME, hubUsername.getAsString());
            configMap.put(HubServerField.HUB_PASSWORD, decryptPassword(hubPassword.getAsString()));
            configMap.put(HubServerField.HUB_TIMEOUT, hubTimeout.getAsString());
            configMap.put(HubServerField.HUB_TRUST_CERT, hubTrustCert.getAsString());

            configMap.put(HubServerField.HUB_PROXY_HOST, hubProxyHost.getAsString());
            configMap.put(HubServerField.HUB_PROXY_PORT, hubProxyPort.getAsString());
            configMap.put(HubServerField.HUB_PROXY_USERNAME, hubProxyUsername.getAsString());
            configMap.put(HubServerField.HUB_PROXY_PASSWORD, decryptPassword(hubProxyPassword.getAsString()));
        }
        return configMap;
    }

    // TODO reuse stream logic
    public void saveConfigMap(final Map<HubServerField, String> configMap) throws IntegrationException {
        final Map<String, String> propertyMap = configMap.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getKey(), e -> e.getValue()));
        encryptPassword(propertyMap, HubServerField.HUB_PASSWORD);
        encryptPassword(propertyMap, HubServerField.HUB_PROXY_PASSWORD);

        final String configJson = gson.toJson(propertyMap);
        try {
            final File configFile = getConfigFile();
            FileUtils.write(configFile, configJson, false);
        } catch (final IOException e) {
            throw new IntegrationException(e);
        }
    }

    private File getConfigFile() throws IntegrationException, IOException {
        final File nexusDir = new File(baseDirectoryName);
        final File configDir = new File(nexusDir, HUB_SERVER_CONFIG_DIRECTORY_PATH);
        if (!configDir.exists()) {
            if (!configDir.mkdirs()) {
                throw new IntegrationException("Could not create the necessary directories for the configuration");
            }
        }
        final File configFile = new File(HUB_SERVER_CONFIG_FILE_NAME);
        if (!configFile.exists()) {
            if (!configFile.createNewFile()) {
                throw new IntegrationException("Could not create the configuration file");
            }
        }
        return configFile;
    }

    private void encryptPassword(final Map<String, String> map, final HubServerField field) throws EncryptionException {
        final String key = field.getKey();
        if (map.containsKey(key)) {
            map.put(key, encryptPassword(map.get(key)));
        }
    }

    private String encryptPassword(final String password) throws EncryptionException {
        if (StringUtils.isNotBlank(password)) {
            return encryptor.alterString(password, null, Cipher.ENCRYPT_MODE);
        }
        return "";
    }

    private String decryptPassword(final String password) throws EncryptionException {
        if (StringUtils.isNotBlank(password)) {
            return encryptor.alterString(password, null, Cipher.DECRYPT_MODE);
        }
        return "";
    }

}
