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
    public PhoneHome(BlackDuckConnection blackDuckConnection) {
        this.blackDuckConnection = blackDuckConnection;
    }

    public BlackDuckPhoneHomeHelper createBlackDuckPhoneHomeHelper(ExecutorService executorService) throws IntegrationException {
        BlackDuckServicesFactory blackDuckServicesFactory = blackDuckConnection.getBlackDuckServicesFactory();
        return BlackDuckPhoneHomeHelper.createAsynchronousPhoneHomeHelper(blackDuckServicesFactory, executorService);
    }

    public PhoneHomeResponse sendDataHome(String taskName, BlackDuckPhoneHomeHelper blackDuckPhoneHomeHelper) {
        Map<String, String> metaData = new HashMap();
        metaData.put("task.type", taskName);

        Version version = FrameworkUtil.getBundle(getClass()).getVersion();
        String productVersion = version.toString();
        String artifactId = FrameworkUtil.getBundle(getClass()).getSymbolicName();
        logger.debug("Found {} version {}", artifactId, productVersion);

        return blackDuckPhoneHomeHelper.handlePhoneHome(artifactId, productVersion, metaData);
    }
}
