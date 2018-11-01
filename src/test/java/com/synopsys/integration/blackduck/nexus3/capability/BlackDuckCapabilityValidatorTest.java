package com.synopsys.integration.blackduck.nexus3.capability;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class BlackDuckCapabilityValidatorTest {

    @Test
    public void validateValidCapabilityTest() {
        final Map<String, String> capabilitySettings = new HashMap<>();

        final String url = "http://google.com";
        final String timeout = "300";
        final String apiKey = "apiKey";
        final String proxyHost = "proxyHost.com";
        final String proxyPort = "80";
        final String proxyUsername = "proxyUsername";
        final String proxyPassword = "proxyPassword";

        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_URL.getKey(), url);
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_TIMEOUT.getKey(), timeout);
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_API_KEY.getKey(), apiKey);
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_HOST.getKey(), proxyHost);
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PORT.getKey(), proxyPort);
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_USERNAME.getKey(), proxyUsername);
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PASSWORD.getKey(), proxyPassword);

        final BlackDuckCapabilityValidator blackDuckCapabilityValidator = new BlackDuckCapabilityValidator();
        try {
            blackDuckCapabilityValidator.validateCapability(capabilitySettings);
        } catch (final IllegalArgumentException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void missingBasicDataTest() {
        final Map<String, String> capabilitySettings = Collections.emptyMap();
        List<String> errors = Collections.emptyList();

        final BlackDuckCapabilityValidator blackDuckCapabilityValidator = new BlackDuckCapabilityValidator();
        try {
            blackDuckCapabilityValidator.validateCapability(capabilitySettings);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            errors = parseErrors(e.getMessage());
        }

        Assert.assertFalse(errors.isEmpty());
        Assert.assertEquals(3, errors.size());

        for (final String error : errors) {
            final boolean missingUrl = error.contains("URL: No Black Duck Url was found.");
            final boolean missingTimeout = error.contains("Timeout: No Black Duck Timeout was found.");
            final boolean missingApiKey = error.contains("API token: No api token was found.");
            Assert.assertTrue(missingUrl || missingApiKey || missingTimeout);
        }
    }

    @Test
    public void badProxyDataTest() {
        final Map<String, String> capabilitySettings = new HashMap<>();

        final String url = "http://google.com";
        final String timeout = "300";
        final String apiKey = "apiKey";
        final String proxyHost = "http://proxyHost.com";
        final String proxyPort = "80";
        final String proxyUsername = "proxyUsername";
        final String proxyPassword = "proxyPassword";

        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_URL.getKey(), url);
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_TIMEOUT.getKey(), timeout);
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_API_KEY.getKey(), apiKey);
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PORT.getKey(), proxyPort);
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_USERNAME.getKey(), proxyUsername);
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PASSWORD.getKey(), proxyPassword);

        final BlackDuckCapabilityValidator blackDuckCapabilityValidator = new BlackDuckCapabilityValidator();
        List<String> errors = Collections.emptyList();

        try {
            blackDuckCapabilityValidator.validateCapability(capabilitySettings);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            errors = parseErrors(e.getMessage());
        }

        Assert.assertFalse(errors.isEmpty());
        Assert.assertEquals(1, errors.size());

        final String error = errors.get(0);
        Assert.assertEquals("Proxy Host: The proxy host not specified.", error);

        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_HOST.getKey(), proxyHost);
        capabilitySettings.remove(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PORT);
    }

    private List<String> parseErrors(final String exceptionMessage) {
        final String[] errorMessages = exceptionMessage.split("  ");
        return Arrays.asList(errorMessages);
    }

    private List<String> failedValidation(final Map<String, String> capabilitySettings) {
        List<String> errors = Collections.emptyList();
        final BlackDuckCapabilityValidator blackDuckCapabilityValidator = new BlackDuckCapabilityValidator();
        try {
            blackDuckCapabilityValidator.validateCapability(capabilitySettings);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            errors = parseErrors(e.getMessage());
        }
        Assert.assertFalse(errors.isEmpty());
        return errors;
    }

}
