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
package com.synopsys.integration.blackduck.nexus3.rest.service;

import com.synopsys.integration.blackduck.nexus3.rest.api.Asset;

public class AssetService extends NexusService<Asset> {
    // private final Logger logger = LoggerFactory.getLogger(AssetService.class);
    //
    // public AssetService(final RestConnection restConnection) {
    // super("/beta/assets", restConnection);
    // }
    //
    // public List<Asset> getAll(final String repositoryName) {
    // final NexusRequestParameter parameter = NexusRequestParameter.repository(repositoryName);
    // try {
    // return getMultipleResponse(parameter);
    // } catch (final IntegrationException e) {
    // logger.error("Problem getting assets.", e);
    // }
    // return Collections.emptyList();
    // }

}
