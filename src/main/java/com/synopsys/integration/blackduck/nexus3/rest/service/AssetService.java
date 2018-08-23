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
package com.synopsys.integration.blackduck.nexus3.rest.service;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.rest.connection.RestConnection;
import com.synopsys.integration.blackduck.nexus3.rest.api.Asset;
import com.synopsys.integration.blackduck.nexus3.rest.api.NexusRequestParameter;

public class AssetService extends NexusService<Asset> {
    private final Logger logger = LoggerFactory.getLogger(AssetService.class);

    public AssetService(final RestConnection restConnection) {
        super("/beta/assets", restConnection);
    }

    public List<Asset> getAll(final String repositoryName) {
        final NexusRequestParameter parameter = NexusRequestParameter.repository(repositoryName);
        try {
            return getMultipleResponse(parameter);
        } catch (final IntegrationException e) {
            logger.error("Problem getting assets.", e);
        }
        return Collections.emptyList();
    }

}
