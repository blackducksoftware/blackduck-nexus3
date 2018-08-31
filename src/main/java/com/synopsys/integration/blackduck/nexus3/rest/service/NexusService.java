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

import com.synopsys.integration.blackduck.nexus3.rest.api.NexusResponse;

public abstract class NexusService<T extends NexusResponse> {
    // private final String servicePath;
    // private final RestConnection restConnection;
    //
    // public NexusService(final String servicePath, final RestConnection restConnection) {
    // this.servicePath = servicePath;
    // this.restConnection = restConnection;
    // }
    //
    // protected final Asset getSingleResponse(final NexusRequestParameter... requestParams) {
    // // FIXME
    // return null;
    // }
    //
    // protected final List<T> getMultipleResponse(final NexusRequestParameter... requestParams) throws IntegrationException {
    // final List<T> allItems = new ArrayList<>();
    // String continuationToken = null;
    // NexusItemsResponse<T> currentBatch;
    // do {
    // currentBatch = getItemsResponse(getRequestUri(), continuationToken, requestParams);
    // if (currentBatch != null) {
    // continuationToken = currentBatch.continuationToken;
    // allItems.addAll(currentBatch.items);
    // }
    // } while (currentBatch.continuationToken != null);
    // return allItems;
    // }
    //
    // private NexusItemsResponse<T> getItemsResponse(final String uri, @Nullable final String continuationToken, final NexusRequestParameter... requestParams) throws IntegrationException {
    // final Request.Builder requestBuilder = new Request.Builder(uri);
    // if (continuationToken != null) {
    // requestBuilder.addQueryParameter("continuationToken", continuationToken);
    // }
    // addQueryParameters(requestBuilder, requestParams);
    //
    // NexusItemsResponse<T> nexusItemsResponse = null;
    // try (final Response response = restConnection.executeRequest(requestBuilder.build());) {
    // final String json = response.getContentString();
    // nexusItemsResponse = restConnection.gson.fromJson(json, NexusItemsResponse.class);
    // } catch (final IOException e) {
    // throw new IntegrationException(e);
    // }
    // return nexusItemsResponse;
    // }
    //
    // private void addQueryParameters(final Request.Builder requestBuilder, final NexusRequestParameter... requestParams) {
    // if (requestParams != null) {
    // for (final NexusRequestParameter param : requestParams) {
    // // FIXME these should not be query parameters
    // requestBuilder.addQueryParameter(param.getName(), param.getDescription());
    // }
    // }
    // }
    //
    // private String getRequestUri() {
    // final URL url = restConnection.baseUrl;
    // return url.toString() + servicePath;
    // }

}
