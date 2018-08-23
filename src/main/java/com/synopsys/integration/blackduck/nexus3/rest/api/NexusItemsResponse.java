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
package com.synopsys.integration.blackduck.nexus3.rest.api;

import java.util.List;

import com.blackducksoftware.integration.util.Stringable;

public class NexusItemsResponse<T extends NexusResponse> extends Stringable {
    public List<T> items;
    public String continuationToken;

}
