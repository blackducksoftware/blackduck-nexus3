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
package com.synopsys.integration.blackduck.nexus3.ui;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.Asset;

public class AssetPanel {
    public static final String BLACKDUCK_CATEGORY = "BlackDuck";

    private final NestedAttributesMap blackDuckNestedAttributes;

    public AssetPanel(final Asset asset) {
        blackDuckNestedAttributes = getBlackDuckNestedAttributes(asset.attributes());
    }

    public String getFromBlackDuckPanel(final AssetPanelLabel label) {
        return (String) blackDuckNestedAttributes.get(label.getLabel());
    }

    public void addToBlackDuckPanel(final AssetPanelLabel label, final Object value) {
        blackDuckNestedAttributes.set(label.getLabel(), value);
    }

    public void removeFromBlackDuckPanel(final AssetPanelLabel label) {
        blackDuckNestedAttributes.remove(label.getLabel());
    }

    // This is used to Add items to the Black Duck tab in the UI
    private NestedAttributesMap getBlackDuckNestedAttributes(final NestedAttributesMap nestedAttributesMap) {
        return nestedAttributesMap.child(BLACKDUCK_CATEGORY);
    }
}
