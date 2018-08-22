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
package com.blackducksoftware.integration.hub.nexus3.rest.service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.nexus3.rest.api.Asset;
import com.blackducksoftware.integration.hub.nexus3.rest.api.NexusItemsResponse;
import com.blackducksoftware.integration.hub.nexus3.rest.api.NexusRequestParameter;
import com.blackducksoftware.integration.hub.nexus3.rest.api.NexusResponse;
import com.blackducksoftware.integration.rest.connection.RestConnection;
import com.blackducksoftware.integration.rest.request.Request;
import com.blackducksoftware.integration.rest.request.Response;

public abstract class NexusService<T extends NexusResponse> {
    private final String servicePath;
    private final RestConnection restConnection;

    public NexusService(final String servicePath, final RestConnection restConnection) {
        this.servicePath = servicePath;
        this.restConnection = restConnection;
    }

    protected final Asset getSingleResponse(final NexusRequestParameter... requestParams) {
        // FIXME
        return null;
    }

    protected final List<T> getMultipleResponse(final NexusRequestParameter... requestParams) throws IntegrationException {
        final List<T> allItems = new ArrayList<>();
        String continuationToken = null;
        NexusItemsResponse<T> currentBatch;
        do {
            currentBatch = getItemsResponse(getRequestUri(), continuationToken, requestParams);
            if (currentBatch != null) {
                continuationToken = currentBatch.continuationToken;
                allItems.addAll(currentBatch.items);
            }
        } while (currentBatch.continuationToken != null);
        return allItems;
    }

    private NexusItemsResponse<T> getItemsResponse(final String uri, @Nullable final String continuationToken, final NexusRequestParameter... requestParams) throws IntegrationException {
        final Request.Builder requestBuilder = new Request.Builder(uri);
        if (continuationToken != null) {
            requestBuilder.addQueryParameter("continuationToken", continuationToken);
        }
        addQueryParameters(requestBuilder, requestParams);

        NexusItemsResponse<T> nexusItemsResponse = null;
        try (final Response response = restConnection.executeRequest(requestBuilder.build());) {
            final String json = response.getContentString();
            nexusItemsResponse = restConnection.gson.fromJson(json, NexusItemsResponse.class);
        } catch (final IOException e) {
            throw new IntegrationException(e);
        }
        return nexusItemsResponse;
    }

    private void addQueryParameters(final Request.Builder requestBuilder, final NexusRequestParameter... requestParams) {
        if (requestParams != null) {
            for (final NexusRequestParameter param : requestParams) {
                // FIXME these should not be query parameters
                requestBuilder.addQueryParameter(param.getName(), param.getDescription());
            }
        }
    }

    private String getRequestUri() {
        final URL url = restConnection.baseUrl;
        return url.toString() + servicePath;
    }

}
