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
package com.blackducksoftware.integration.hub.nexus3.rest.api;

import java.util.List;

public class Component extends NexusResponse {
    public String id;
    public String repository;
    public String format;
    public String group;
    public String name;
    public String version;
    public List<Asset> assets;

}
