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
package com.synopsys.integration.blackduck.nexus3.database;

import java.util.Optional;

public class PagedResult<TYPE> {
    private final Iterable<TYPE> typeList;
    private final Optional<String> lastName;

    public PagedResult(final Iterable<TYPE> typeList, final Optional<String> lastName) {
        this.typeList = typeList;
        this.lastName = lastName;
    }

    public Iterable<TYPE> getTypeList() {
        return typeList;
    }

    public Optional<String> getLastName() {
        return lastName;
    }

    public boolean hasResults() {
        return (typeList != null) && typeList.iterator().hasNext();
    }
}
