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
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.browse.BrowseService;

@Named
public class BlackDuckScanTask extends RepositoryTaskSupport {
    private static final String TEST_KEY = "BD_Test";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private BrowseService browseService;

    @Inject

    public BlackDuckScanTask(BrowseService browseService) {
        this.browseService = browseService;
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
        //        StorageTx storageTx = repository.facet(AttributesFacetImpl.class).txSupplier().get();
        //        QueryOptions queryOptions = new QueryOptions(null, null, null, null, null);
        //        BrowseResult<Component> foundComponentsResult = browseService.browseComponents(repository, queryOptions);
        //        List<Component> foundComponents = foundComponentsResult.getResults();
        //        foundComponents.forEach(component -> {
        //            boolean switched = true;
        //            String foundValue = (String) component.attributes().get(TEST_KEY);
        //            if (StringUtils.isNotBlank(foundValue)) {
        //                switched = !Boolean.parseBoolean(foundValue);
        //            }
        //            component.attributes().set(TEST_KEY, switched);
        //            storageTx.saveComponent(component);
        //        });
    }

}
