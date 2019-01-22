package com.synopsys.integration.blackduck.nexus3.task.common;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.nexus3.BlackDuckConnection;
import com.synopsys.integration.blackduck.phonehome.BlackDuckPhoneHomeHelper;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.phonehome.PhoneHomeResponse;

@Named
@Singleton
public class PhoneHome {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final BlackDuckConnection blackDuckConnection;

    @Inject
    public PhoneHome(final BlackDuckConnection blackDuckConnection) {
        this.blackDuckConnection = blackDuckConnection;
    }

    public BlackDuckPhoneHomeHelper createBlackDuckPhoneHomeHelper(final ExecutorService executorService) throws IntegrationException {
        final BlackDuckServicesFactory blackDuckServicesFactory = blackDuckConnection.getBlackDuckServicesFactory();
        return BlackDuckPhoneHomeHelper.createAsynchronousPhoneHomeHelper(blackDuckServicesFactory, executorService);
    }

    public PhoneHomeResponse sendDataHome(final String taskName, final BlackDuckPhoneHomeHelper blackDuckPhoneHomeHelper) throws IntegrationException {
        final Map<String, String> metaData = new HashMap();
        metaData.put("task.type", taskName);

        final Version version = FrameworkUtil.getBundle(getClass()).getVersion();
        final String productVersion = version.toString();
        final String artifactId = FrameworkUtil.getBundle(getClass()).getSymbolicName();
        logger.debug("Found {} version {}", artifactId, productVersion);

        return blackDuckPhoneHomeHelper.handlePhoneHome(artifactId, productVersion, metaData);
    }
}
