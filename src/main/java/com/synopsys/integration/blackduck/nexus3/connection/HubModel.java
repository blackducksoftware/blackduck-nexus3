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
package com.synopsys.integration.blackduck.nexus3.connection;

public class HubModel {
    private String hubUrl;
    private int timeout;
    private String apiKey;
    private boolean trustCertificate;

    public HubModel() {
    }

    public HubModel(final String hubUrl, final String apiKey, final boolean trustCertificate, final int timeout) {
        this.hubUrl = hubUrl;
        this.apiKey = apiKey;
        this.trustCertificate = trustCertificate;
        this.timeout = timeout;
    }

    public String getHubUrl() {
        return hubUrl;
    }

    public void setHubUrl(final String hubUrl) {
        this.hubUrl = hubUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isTrustCertificate() {
        return trustCertificate;
    }

    public void setTrustCertificate(final String trustCertificate) {
        this.trustCertificate = Boolean.parseBoolean(trustCertificate);
    }

    public void setTrustCertificate(final boolean trustCertificate) {
        this.trustCertificate = trustCertificate;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    public void setTimeout(final String timeout) {
        this.timeout = Integer.parseInt(timeout);
    }
}
