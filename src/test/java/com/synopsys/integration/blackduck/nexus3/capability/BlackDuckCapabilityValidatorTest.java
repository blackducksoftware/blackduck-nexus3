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

        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_URL.getKey(), "http://google.com");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_TIMEOUT.getKey(), "300");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_API_KEY.getKey(), "apiKey");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_HOST.getKey(), "proxyHost.com");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PORT.getKey(), "80");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_USERNAME.getKey(), "proxyUsername");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PASSWORD.getKey(), "proxyPassword");

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

        final List<String> errors = failedValidation(capabilitySettings);
        Assert.assertEquals(3, errors.size());

        for (final String error : errors) {
            final boolean missingUrl = error.contains("URL: No Black Duck Url was found.");
            final boolean missingTimeout = error.contains("Timeout: No Black Duck Timeout was found.");
            final boolean missingApiKey = error.contains("API token: No api token was found.");
            Assert.assertTrue(missingUrl || missingApiKey || missingTimeout);
        }
    }

    @Test
    public void badProxyUrlDataTest() {
        final Map<String, String> capabilitySettings = new HashMap<>();

        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_URL.getKey(), "http://google.com");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_TIMEOUT.getKey(), "300");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_API_KEY.getKey(), "apiKey");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PORT.getKey(), "80");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_USERNAME.getKey(), "proxyUsername");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PASSWORD.getKey(), "proxyPassword");

        final List<String> proxyHostErrors = failedValidation(capabilitySettings);
        Assert.assertEquals(1, proxyHostErrors.size());

        final String proxyHostError = proxyHostErrors.get(0);
        Assert.assertEquals("Proxy Host: The proxy host not specified.", proxyHostError);

        final String proxyHost = "proxyHost.com";

        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_HOST.getKey(), proxyHost);
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PORT.getKey(), "-1");

        final List<String> proxyPortNumberErrors = failedValidation(capabilitySettings);
        Assert.assertEquals(1, proxyPortNumberErrors.size());

        final String proxyPortNumberError = proxyPortNumberErrors.get(0);
        Assert.assertEquals("Proxy Port: The proxy port must be greater than 0.", proxyPortNumberError);

        final String notANumber = "notanumber";
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PORT.getKey(), notANumber);

        final List<String> proxyPortNotNumberErrors = failedValidation(capabilitySettings);
        Assert.assertEquals(1, proxyPortNotNumberErrors.size());

        final String proxyPortNotNumberError = proxyPortNotNumberErrors.get(0);
        Assert.assertEquals(String.format("Proxy Port: The String : %s, is not an Integer.", notANumber), proxyPortNotNumberError);

        capabilitySettings.remove(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PORT.getKey());

        final List<String> proxyPortErrors = failedValidation(capabilitySettings);
        Assert.assertEquals(1, proxyHostErrors.size());

        final String proxyPortError = proxyPortErrors.get(0);
        Assert.assertEquals("Proxy Port: The proxy port not specified.", proxyPortError);
    }

    @Test
    public void badProxyCredentialsDataTest() {
        final Map<String, String> capabilitySettings = new HashMap<>();

        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_URL.getKey(), "http://google.com");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_TIMEOUT.getKey(), "300");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_API_KEY.getKey(), "apiKey");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_HOST.getKey(), "proxyHost.com");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PORT.getKey(), "80");
        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PASSWORD.getKey(), "proxyPassword");

        final List<String> proxyUsernameErrors = failedValidation(capabilitySettings);
        Assert.assertEquals(1, proxyUsernameErrors.size());

        final String usernameError = proxyUsernameErrors.get(0);
        Assert.assertEquals("Proxy Username: The proxy user not specified.", usernameError);

        capabilitySettings.put(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_USERNAME.getKey(), "proxyUsername");
        capabilitySettings.remove(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PASSWORD.getKey());

        final List<String> proxyPasswordErrors = failedValidation(capabilitySettings);
        Assert.assertEquals(1, proxyPasswordErrors.size());

        final String proxyPasswordError = proxyPasswordErrors.get(0);
        Assert.assertEquals("Proxy Password: The proxy password not specified.", proxyPasswordError);
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
