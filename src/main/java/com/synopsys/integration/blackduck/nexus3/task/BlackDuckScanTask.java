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
package com.synopsys.integration.blackduck.nexus3.task;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.storage.Asset;

import com.synopsys.integration.blackduck.nexus3.database.QueryManager;

@Named
public class BlackDuckScanTask extends RepositoryTaskSupport {
    private static final String TEST_KEY = "BD_Test";
    private static final String BLACKDUCK_CATEGORY = "Black Duck";
    private final Logger logger = createLogger();

    private QueryManager queryManager;

    @Inject
    public BlackDuckScanTask(QueryManager queryManager) {
        this.queryManager = queryManager;
    }

    @Override
    public String getMessage() {
        return "BlackDuck scanning repository " + getRepositoryField();
    }

    @Override
    protected boolean appliesTo(final Repository repository) {
        final String repositoryName = getRepositoryField();
        return repository.getName().equals(repositoryName);
    }

    @Override
    protected void execute(final Repository repository) {
        logger.info("Found repository: " + repository.getName());
        Iterable<Asset> foundAssets = queryManager.findAssetsInRepository(repository);
        for (Asset asset : foundAssets) {
            if (asset.componentId() != null) {
                logger.info("Scanning item: " + asset.name());
                // Scan item
                NestedAttributesMap blackDuckNestedAttributes = getBlackDuckNestedAttributes(asset.attributes());
                Boolean switched = true;
                Boolean foundValue = (Boolean) blackDuckNestedAttributes.get(TEST_KEY);
                if (foundValue != null) {
                    switched = !foundValue;
                    logger.info(String.format("Switching value from %s to %s", foundValue, switched));
                }
                blackDuckNestedAttributes.set(TEST_KEY, switched);
                logger.info("Saving switched asset");
                queryManager.updateAsset(repository, asset);
            }
        }
    }

    public NestedAttributesMap getBlackDuckNestedAttributes(NestedAttributesMap nestedAttributesMap) {
        return nestedAttributesMap.child(BLACKDUCK_CATEGORY);
    }

}
