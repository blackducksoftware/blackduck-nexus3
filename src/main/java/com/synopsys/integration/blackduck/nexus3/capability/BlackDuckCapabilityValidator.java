package com.synopsys.integration.blackduck.nexus3.capability;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

public class BlackDuckCapabilityValidator {
    public void validateCapability(final Map<String, String> capabilitySettings) {
        final Optional<String> urlError = validateBlackDuckUrl(capabilitySettings);
        final Optional<String> timeoutError = validateBlackDuckTimeout(capabilitySettings);
        final Optional<String> apiKeyError = validateBlackDuckCredentials(capabilitySettings);
        final Optional<String> proxyError = validateProxySettings(capabilitySettings);
        final List<String> errors = Stream.of(urlError, timeoutError, apiKeyError, proxyError).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(StringUtils.join(errors, "  "));
        }
    }

    private Optional<String> validateBlackDuckUrl(final Map<String, String> capabilitySettings) {
        final String configuredUrl = capabilitySettings.get(BlackDuckCapabilityConfigKeys.BLACKDUCK_URL.getKey());
        if (StringUtils.isBlank(configuredUrl)) {
            return Optional.of("URL: No Black Duck Url was found.");
        } else {
            try {
                final URL hubURL = new URL(configuredUrl);
                hubURL.toURI();
            } catch (final MalformedURLException | URISyntaxException e) {
                return Optional.of("URL: The Black Duck Url is not a valid URL.");
            }
        }
        return Optional.empty();
    }

    private Optional<String> validateBlackDuckTimeout(final Map<String, String> capabilitySettings) {
        final String configuredTimeout = capabilitySettings.get(BlackDuckCapabilityConfigKeys.BLACKDUCK_TIMEOUT.getKey());
        if (StringUtils.isBlank(configuredTimeout)) {
            return Optional.of("Timeout: No Black Duck Timeout was found.");
        } else {
            try {
                final Integer timeout = Integer.valueOf(configuredTimeout);
                if (timeout <= 0) {
                    return Optional.of("Timeout: The timeout must be greater than 0.");
                }
            } catch (final NumberFormatException e) {
                return Optional.of(String.format("Timeout: The String : %s, is not an Integer.", configuredTimeout));
            }
        }
        return Optional.empty();
    }

    private Optional<String> validateBlackDuckCredentials(final Map<String, String> capabilitySettings) {
        final String configuredApiKey = capabilitySettings.get(BlackDuckCapabilityConfigKeys.BLACKDUCK_API_KEY.getKey());
        if (StringUtils.isBlank(configuredApiKey)) {
            return Optional.of("API token: No api token was found.");
        }
        return Optional.empty();
    }

    private Optional<String> validateProxySettings(final Map<String, String> capabilitySettings) {
        final String configuredProxyHost = capabilitySettings.get(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_HOST.getKey());
        final String configuredProxyPort = capabilitySettings.get(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PORT.getKey());
        final String configuredProxyUser = capabilitySettings.get(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_USERNAME.getKey());
        final String configuredProxyPassword = capabilitySettings.get(BlackDuckCapabilityConfigKeys.BLACKDUCK_PROXY_PASSWORD.getKey());

        if (StringUtils.isBlank(configuredProxyHost) && (StringUtils.isNotBlank(configuredProxyPort) || StringUtils.isNotBlank(configuredProxyUser) || StringUtils.isNotBlank(configuredProxyPassword))) {
            return Optional.of("Proxy Host: The proxy host not specified.");
        } else {
            if (StringUtils.isNotBlank(configuredProxyHost) && StringUtils.isBlank(configuredProxyPort)) {
                return Optional.of("Proxy Port: The proxy port not specified.");
            } else if (StringUtils.isNotBlank(configuredProxyHost) && StringUtils.isNotBlank(configuredProxyPort)) {
                try {
                    final Integer timeout = Integer.valueOf(configuredProxyPort);
                    if (timeout <= 0) {
                        return Optional.of("Proxy Port: The proxy port must be greater than 0.");
                    }
                } catch (final NumberFormatException e) {
                    return Optional.of(String.format("Proxy Port: The String : %s, is not an Integer.", configuredProxyPort));
                }
            }
            if (StringUtils.isNotBlank(configuredProxyUser) && StringUtils.isBlank(configuredProxyPassword)) {
                return Optional.of("Proxy Password: The proxy password not specified.");
            } else if (StringUtils.isBlank(configuredProxyUser) && StringUtils.isNotBlank(configuredProxyPassword)) {
                return Optional.of("Proxy Username: The proxy user not specified.");
            }
        }
        return Optional.empty();
    }
}
